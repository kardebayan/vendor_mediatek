
//#define LOG_NDEBUG 0
#define LOG_TAG "[sink][recorder]MediaMuxer"
#include <utils/Log.h>



#include <media/stagefright/foundation/ABuffer.h>
#include <media/stagefright/foundation/ADebug.h>
#include <media/stagefright/foundation/AMessage.h>
#include <media/stagefright/MediaBuffer.h>
#include <media/stagefright/MediaCodec.h>
#include <media/stagefright/MediaDefs.h>
#include <media/stagefright/MediaErrors.h>
#include <media/MediaSource.h>
#include <media/stagefright/MetaData.h>
#include <media/stagefright/MPEG4Writer.h>
#include <media/stagefright/Utils.h>


#include "MediaAdapter.h"
#include "MediaMuxer.h"
#include "comutils.h"

namespace android
{


MediaMuxer::MediaMuxer(int fd, OutputFormat format)
    : mFormat(format),
      mState(UNINITIALIZED)
{
    if(format == OUTPUT_FORMAT_MPEG_4) {
        mWriter = new MPEG4Writer(fd);
    } /*else if (format == OUTPUT_FORMAT_WEBM) {

        mWriter = new WebmWriter(fd);
    }*/

    if(mWriter != NULL) {
        mFileMeta = new MetaData;
        mState = INITIALIZED;
    }
}

MediaMuxer::~MediaMuxer()
{
    Mutex::Autolock autoLock(mMuxerLock);

    // Clean up all the internal resources.
    mFileMeta.clear();
    mWriter.clear();
    mTrackList.clear();
}

ssize_t MediaMuxer::addTrack(const sp<AMessage> &format)
{
    Mutex::Autolock autoLock(mMuxerLock);

    if(format.get() == NULL) {
        ALOGE("addTrack() get a null format");
        return -EINVAL;
    }

    if(mState != INITIALIZED) {
        ALOGE("addTrack() must be called after constructor and before start().");
        return INVALID_OPERATION;
    }

    sp<MetaData> trackMeta = new MetaData;
    convertMessageToMetaData(format, trackMeta);

    sp<MediaAdapter> newTrack = new MediaAdapter(trackMeta);
    status_t result = mWriter->addSource(newTrack);

    if(result == OK) {
        return mTrackList.add(newTrack);
    }

    return -1;
}

status_t MediaMuxer::setOrientationHint(int degrees)
{
    Mutex::Autolock autoLock(mMuxerLock);

    if(mState != INITIALIZED) {
        ALOGE("setOrientationHint() must be called before start().");
        return INVALID_OPERATION;
    }

    if(degrees != 0 && degrees != 90 && degrees != 180 && degrees != 270) {
        ALOGE("setOrientationHint() get invalid degrees");
        return -EINVAL;
    }

    mFileMeta->setInt32(kKeyRotation, degrees);
    return OK;
}

status_t MediaMuxer::setLocation(int latitude, int longitude)
{
    Mutex::Autolock autoLock(mMuxerLock);

    if(mState != INITIALIZED) {
        ALOGE("setLocation() must be called before start().");
        return INVALID_OPERATION;
    }

    if(mFormat != OUTPUT_FORMAT_MPEG_4) {
        ALOGE("setLocation() is only supported for .mp4 output.");
        return INVALID_OPERATION;
    }

    ALOGV("Setting location: latitude = %d, longitude = %d", latitude, longitude);
    return static_cast<MPEG4Writer*>(mWriter.get())->setGeoData(latitude, longitude);
}

status_t MediaMuxer::start()
{
    Mutex::Autolock autoLock(mMuxerLock);

    if(mState == INITIALIZED) {
        mState = STARTED;
        mFileMeta->setInt32(kKeyRealTimeRecording, false);
        return mWriter->start(mFileMeta.get());
    } else {
        ALOGE("start() is called in invalid state %d", mState);
        return INVALID_OPERATION;
    }
}

status_t MediaMuxer::stop()
{
    Mutex::Autolock autoLock(mMuxerLock);

    if(mState == STARTED) {
        mState = STOPPED;

        for(size_t i = 0; i < mTrackList.size(); i++) {
            if(mTrackList[i]->stop() != OK) {
                return INVALID_OPERATION;
            }
        }

        return mWriter->stop();
    } else {
        ALOGE("stop() is called in invalid state %d", mState);
        return INVALID_OPERATION;
    }
}

status_t MediaMuxer::writeSampleData(const sp<ABuffer> &buffer, size_t trackIndex,
                                     int64_t timeUs, uint32_t flags)
{
    Mutex::Autolock autoLock(mMuxerLock);

    if(buffer.get() == NULL) {
        ALOGE("WriteSampleData() get an NULL buffer.");
        return -EINVAL;
    }

    if(mState != STARTED) {
        ALOGE("WriteSampleData() is called in invalid state %d", mState);
        return INVALID_OPERATION;
    }

    if(trackIndex >= mTrackList.size()) {
        ALOGE("WriteSampleData() get an invalid index %zu", trackIndex);
        return -EINVAL;
    }

    MediaBuffer* mediaBuffer = new MediaBuffer(buffer);

    mediaBuffer->add_ref(); // Released in MediaAdapter::signalBufferReturned().
    mediaBuffer->set_range(buffer->offset(), buffer->size());

    MetaDataBase &sampleMetaData = mediaBuffer->meta_data();
    sampleMetaData.setInt64(kKeyTime, timeUs);
    // Just set the kKeyDecodingTime as the presentation time for now.
    sampleMetaData.setInt64(kKeyDecodingTime, timeUs);

    if(flags &  SAMPLE_FLAG_SYNC) {
        sampleMetaData.setInt32(kKeyIsSyncFrame, true);
    } else if(flags &  SAMPLE_FLAG_CODEC_CONFIG) {
        sampleMetaData.setInt32(kKeyIsCodecConfig, 1);
    }

    sp<MediaAdapter> currentTrack = mTrackList[trackIndex];
    // This pushBuffer will wait until the mediaBuffer is consumed.
    return currentTrack->pushBuffer(mediaBuffer);
}


}  // namespace android
