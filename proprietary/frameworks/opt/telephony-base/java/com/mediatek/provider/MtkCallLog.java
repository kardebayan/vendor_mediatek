/*
 * Copyright (C) 2014 MediaTek Inc.
 * Modification based on code covered by the mentioned copyright
 * and/or permission notice(s).
 */

package com.mediatek.provider;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.database.Cursor;
import android.location.Country;
import android.location.CountryDetector;
import android.net.Uri;
import android.os.Build;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.CallLog;
import android.provider.ContactsContract.CommonDataKinds.Callable;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.DataUsageFeedback;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.telephony.CallerInfo;
import com.android.internal.telephony.PhoneConstants;

import java.util.List;

public class MtkCallLog {
    private static final boolean VERBOSE_LOG = !("user".equals(Build.TYPE));
    private static final String LOG_TAG = "MtkCallLog";

    /**
     * The "shadow" provider stores calllog when the real calllog provider is encrypted.  The
     * real provider will alter copy from it when it starts, and remove the entries in the shadow.
     *
     * <p>See the comment in {@link Calls#addCall} for the details.
     *
     * @hide
     */
    public static final String SHADOW_AUTHORITY = "call_log_shadow";

    public static class Calls extends android.provider.CallLog.Calls {
        /**
         * M: Call log type for auto rejected calls.
         * @hide
         * @internal
         */
        public static final int AUTO_REJECT_TYPE = 8;
        
        /**
         * M: Add a parameter for Volte Conference call Calllog
         * Adds a call to the call log.
         *
         * @param ci the CallerInfo object to get the target contact from.  Can be null
         * if the contact is unknown.
         * @param context the context used to get the ContentResolver
         * @param number the phone number to be added to the calls db
         * @param postDialDigits the post-dial digits that were dialed after the number,
         *        if it was outgoing. Otherwise it is ''.
         * @param viaNumber the secondary number that the incoming call received with. If the
         *        call was received with the SIM assigned number, then this field must be ''.
         * @param presentation enum value from PhoneConstants.PRESENTATION_xxx, which
         *        is set by the network and denotes the number presenting rules for
         *        "allowed", "payphone", "restricted" or "unknown"
         * @param callType enumerated values for "incoming", "outgoing", or "missed"
         * @param features features of the call (e.g. Video).
         * @param accountHandle The accountHandle object identifying the provider of the call
         * @param start time stamp for the call in milliseconds
         * @param duration call duration in seconds
         * @param dataUsage data usage for the call in bytes, null if data usage was not tracked for
         *                  the call.
         * @param addForAllUsers If true, the call is added to the call log of all currently
         *        running users. The caller must have the MANAGE_USERS permission if this is true.
         * @param userToBeInsertedTo {@link UserHandle} of user that the call is going to be
         *                           inserted to. null if it is inserted to the current user. The
         *                           value is ignored if @{link addForAllUsers} is true.
         * @param is_read Flag to show if the missed call log has been read by the user or not.
         *                Used for call log restore of missed calls.
         * @param conferenceCallId The conference call id in database.
         * @param conferenceDuration conference duration in seconds
         *
         * @return The URI of the call log entry belonging to the user that made or received this
         *        call.  This could be of the shadow provider.  Do not return it to non-system apps,
         *        as they don't have permissions.
         * {@hide}
         */
        public static Uri addCall(CallerInfo ci, Context context, String number,
                String postDialDigits, String viaNumber, int presentation, int callType,
                int features, PhoneAccountHandle accountHandle, long start, int duration,
                Long dataUsage, boolean addForAllUsers, UserHandle userToBeInsertedTo,
                boolean is_read, long conferenceCallId, int conferenceDuration) {
            if (VERBOSE_LOG) {
                Log.v(LOG_TAG, String.format("Add call: number=%s, user=%s, for all=%s",
                        number, userToBeInsertedTo, addForAllUsers));
            }
            final ContentResolver resolver = context.getContentResolver();
            int numberPresentation = PRESENTATION_ALLOWED;

            TelecomManager tm = null;
            try {
                tm = TelecomManager.from(context);
            } catch (UnsupportedOperationException e) {}

            String accountAddress = null;
            if (tm != null && accountHandle != null) {
                PhoneAccount account = tm.getPhoneAccount(accountHandle);
                if (account != null) {
                    Uri address = account.getSubscriptionAddress();
                    if (address != null) {
                        accountAddress = address.getSchemeSpecificPart();
                    }
                }
            }

            // Remap network specified number presentation types
            // PhoneConstants.PRESENTATION_xxx to calllog number presentation types
            // Calls.PRESENTATION_xxx, in order to insulate the persistent calllog
            // from any future radio changes.
            // If the number field is empty set the presentation type to Unknown.
            if (presentation == PhoneConstants.PRESENTATION_RESTRICTED) {
                numberPresentation = PRESENTATION_RESTRICTED;
            } else if (presentation == PhoneConstants.PRESENTATION_PAYPHONE) {
                numberPresentation = PRESENTATION_PAYPHONE;
            } else if (TextUtils.isEmpty(number)
                    || presentation == PhoneConstants.PRESENTATION_UNKNOWN) {
                numberPresentation = PRESENTATION_UNKNOWN;
            }
            if (numberPresentation != PRESENTATION_ALLOWED) {
                number = "";
                if (ci != null) {
                    ci.name = "";
                }
            }

            // accountHandle information
            String accountComponentString = null;
            String accountId = null;
            if (accountHandle != null) {
                accountComponentString = accountHandle.getComponentName().flattenToString();
                accountId = accountHandle.getId();
            }

            ContentValues values = new ContentValues(6);

            values.put(NUMBER, number);
            values.put(POST_DIAL_DIGITS, postDialDigits);
            values.put(VIA_NUMBER, viaNumber);
            values.put(NUMBER_PRESENTATION, Integer.valueOf(numberPresentation));
            values.put(TYPE, Integer.valueOf(callType));
            values.put(FEATURES, features);
            values.put(DATE, Long.valueOf(start));
            values.put(DURATION, Long.valueOf(duration));
            if (dataUsage != null) {
                values.put(DATA_USAGE, dataUsage);
            }
            values.put(PHONE_ACCOUNT_COMPONENT_NAME, accountComponentString);
            values.put(PHONE_ACCOUNT_ID, accountId);
            values.put(PHONE_ACCOUNT_ADDRESS, accountAddress);
            values.put(NEW, Integer.valueOf(1));
            values.put(ADD_FOR_ALL_USERS, addForAllUsers ? 1 : 0);

            if (callType == MISSED_TYPE) {
                values.put(IS_READ, Integer.valueOf(is_read ? 1 : 0));
            }

            // [ALPS03840626]: Fix BT call log issue
            if (ci != null) {
                values.put(CACHED_NAME, ci.name);
                values.put(CACHED_NUMBER_TYPE, ci.numberType);
                values.put(CACHED_NUMBER_LABEL, ci.numberLabel);
            }

            if ((ci != null) && (ci.contactIdOrZero > 0)) {
                // Update usage information for the number associated with the contact ID.
                // We need to use both the number and the ID for obtaining a data ID since other
                // contacts may have the same number.

                final Cursor cursor;

                // We should prefer normalized one (probably coming from
                // Phone.NORMALIZED_NUMBER column) first. If it isn't available try others.
                if (ci.normalizedNumber != null) {
                    final String normalizedPhoneNumber = ci.normalizedNumber;
                    cursor = resolver.query(Phone.CONTENT_URI,
                            new String[] { Phone._ID },
                            Phone.CONTACT_ID + " =? AND " + Phone.NORMALIZED_NUMBER + " =?",
                            new String[] { String.valueOf(ci.contactIdOrZero),
                                    normalizedPhoneNumber},
                            null);
                } else {
                    final String phoneNumber = ci.phoneNumber != null ? ci.phoneNumber : number;
                    cursor = resolver.query(
                            Uri.withAppendedPath(Callable.CONTENT_FILTER_URI,
                                    Uri.encode(phoneNumber)),
                            new String[] { Phone._ID },
                            Phone.CONTACT_ID + " =?",
                            new String[] { String.valueOf(ci.contactIdOrZero) },
                            null);
                }

                if (cursor != null) {
                    try {
                        if (cursor.getCount() > 0 && cursor.moveToFirst()) {
                            final String dataId = cursor.getString(0);
                            updateDataUsageStatForData(resolver, dataId);
                            if (duration >= MIN_DURATION_FOR_NORMALIZED_NUMBER_UPDATE_MS
                                    && callType == Calls.OUTGOING_TYPE
                                    && TextUtils.isEmpty(ci.normalizedNumber)) {
                                updateNormalizedNumber(context, resolver, dataId, number);
                            }
                        }
                    } finally {
                        cursor.close();
                    }
                }
            }

            /*
                Writing the calllog works in the following way:
                - All user entries
                    - if user-0 is encrypted, insert to user-0's shadow only.
                      (other users should also be encrypted, so nothing to do for other users.)
                    - if user-0 is decrypted, insert to user-0's real provider, as well as
                      all other users that are running and decrypted and should have calllog.

                - Single user entry.
                    - If the target user is encryted, insert to its shadow.
                    - Otherwise insert to its real provider.

                When the (real) calllog provider starts, it copies entries that it missed from
                elsewhere.
                - When user-0's (real) provider starts, it copies from user-0's shadow, and clears
                  the shadow.

                - When other users (real) providers start, unless it shouldn't have calllog entries,
                     - Copy from the user's shadow, and clears the shadow.
                     - Copy from user-0's entries that are FOR_ALL_USERS = 1.  (and don't clear it.)
             */

            Uri result = null;

            final UserManager userManager = context.getSystemService(UserManager.class);
            final int currentUserId = userManager.getUserHandle();

            /// M: if conference child call, only add to current user @{
            if (conferenceCallId > 0) {
                addForAllUsers = false;
            }
            /// @}

            if (addForAllUsers) {
                // First, insert to the system user.
                final Uri uriForSystem = addEntryAndRemoveExpiredEntries(
                        context, userManager, UserHandle.SYSTEM, values);
                if (uriForSystem == null
                        || SHADOW_AUTHORITY.equals(uriForSystem.getAuthority())) {
                    // This means the system user is still encrypted and the entry has inserted
                    // into the shadow.  This means other users are still all encrypted.
                    // Nothing further to do; just return null.
                    return null;
                }
                if (UserHandle.USER_SYSTEM == currentUserId) {
                    result = uriForSystem;
                }

                // Otherwise, insert to all other users that are running and unlocked.

                final List<UserInfo> users = userManager.getUsers(true);

                final int count = users.size();
                for (int i = 0; i < count; i++) {
                    final UserInfo userInfo = users.get(i);
                    final UserHandle userHandle = userInfo.getUserHandle();
                    final int userId = userHandle.getIdentifier();

                    if (userHandle.isSystem()) {
                        // Already written.
                        continue;
                    }

                    if (!shouldHaveSharedCallLogEntries(context, userManager, userId)) {
                        ///M: add log
                        if (VERBOSE_LOG) {
                            Log.v(LOG_TAG, "Shouldn't have calllog entries. userId=" + userId);
                        }
                        // Shouldn't have calllog entries.
                        continue;
                    }

                    // For other users, we write only when they're running *and* decrypted.
                    // Other providers will copy from the system user's real provider, when they
                    // start.
                    if (userManager.isUserRunning(userHandle)
                            && userManager.isUserUnlocked(userHandle)) {
                        final Uri uri = addEntryAndRemoveExpiredEntries(context, userManager,
                                userHandle, values);
                        if (userId == currentUserId) {
                            result = uri;
                        }
                    }
                }
            } else {
                // Single-user entry. Just write to that user, assuming it's running.  If the
                // user is encrypted, we write to the shadow calllog.

                final UserHandle targetUserHandle = userToBeInsertedTo != null
                        ? userToBeInsertedTo
                        : UserHandle.of(currentUserId);
                result = addEntryAndRemoveExpiredEntries(context, userManager, targetUserHandle,
                        values, conferenceCallId, conferenceDuration);
            }
            return result;
        }

        private static void updateDataUsageStatForData(ContentResolver resolver, String dataId) {
            final Uri feedbackUri = DataUsageFeedback.FEEDBACK_URI.buildUpon()
                    .appendPath(dataId)
                    .appendQueryParameter(DataUsageFeedback.USAGE_TYPE,
                                DataUsageFeedback.USAGE_TYPE_CALL)
                    .build();
            resolver.update(feedbackUri, new ContentValues(), null, null);
        }


        /*
         * Update the normalized phone number for the given dataId in the ContactsProvider, based
         * on the user's current country.
         */
        private static void updateNormalizedNumber(Context context, ContentResolver resolver,
                String dataId, String number) {
            if (TextUtils.isEmpty(number) || TextUtils.isEmpty(dataId)) {
                return;
            }
            final String countryIso = getCurrentCountryIso(context);
            if (TextUtils.isEmpty(countryIso)) {
                return;
            }
            final String normalizedNumber = PhoneNumberUtils.formatNumberToE164(number,
                    getCurrentCountryIso(context));
            if (TextUtils.isEmpty(normalizedNumber)) {
                return;
            }
            final ContentValues values = new ContentValues();
            values.put(Phone.NORMALIZED_NUMBER, normalizedNumber);
            resolver.update(Data.CONTENT_URI, values, Data._ID + "=?", new String[] {dataId});
        }

        private static Uri addEntryAndRemoveExpiredEntries(Context context, UserManager userManager,
                UserHandle user, ContentValues values) {
            final ContentResolver resolver = context.getContentResolver();

            // Since we're doing this operation on behalf of an app, we only
            // want to use the actual "unlocked" state.
            final Uri uri = ContentProvider.maybeAddUserId(
                    userManager.isUserUnlocked(user) ? CONTENT_URI : SHADOW_CONTENT_URI,
                    user.getIdentifier());

            if (VERBOSE_LOG) {
                Log.v(LOG_TAG, String.format("Inserting to %s", uri));
            }

            try {
                final Uri result = resolver.insert(uri, values);
                resolver.delete(uri, "_id IN " +
                        "(SELECT _id FROM calls ORDER BY " + DEFAULT_SORT_ORDER
                        + " LIMIT -1 OFFSET 500)", null);
                return result;
            } catch (IllegalArgumentException e) {
                Log.w(LOG_TAG, "Failed to insert calllog", e);
                // Even though we make sure the target user is running and decrypted before calling
                // this method, there's a chance that the user just got shut down, in which case
                // we'll still get "IllegalArgumentException: Unknown URL content://call_log/calls".
                return null;
            }
        }

        /**
         * M: add entry with pre-process for conference call
         * 1. put id to ContentValue and update conference duration if need
         * 2. update call if exist, else insert a new
         */
        private static Uri addEntryAndRemoveExpiredEntries(Context context, UserManager userManager,
                UserHandle user, ContentValues values, long conferenceCallId,
                int conferenceDuration) {
            Log.i(LOG_TAG, "addEntryAndRemoveExpiredEntries conf id " + conferenceCallId);

            if (conferenceCallId > 0) {
                // Add for Volte Conference call Calllog
                // NOTE: CONFERENCE_CALL_ID should be initialized as INTEGER NOT NULL DEFAULT -1
                //       in CallLogDatabaseHelper.java
                values.put(CONFERENCE_CALL_ID, conferenceCallId);
                ConferenceCalls.updateConferenceDurationIfNeeded(context, user, conferenceCallId,
                        conferenceDuration);

                Uri resultUri = updateCallEntryIfExist(context, userManager, user, values,
                        conferenceCallId);
                if (resultUri != null) {
                    if (VERBOSE_LOG) {
                        Log.i(LOG_TAG, "Entry exist, update " + resultUri);
                    }
                    return resultUri;
                }
            }
            return addEntryAndRemoveExpiredEntries(context, userManager, user, values);
        }

        /**
         * M: for conference call, need to check call before insert as it may be added
         * 1. check if call existed with same number, date and conference id
         * 2. If exist update duration only, else insert a new
         */
        private static Uri updateCallEntryIfExist(Context context, UserManager userManager,
                UserHandle user, ContentValues values, long conferenceCallId) {
            // Since we're doing this operation on behalf of an app, we only
            // want to use the actual "unlocked" state.
            final Uri uri = ContentProvider.maybeAddUserId(
                    userManager.isUserUnlocked(user) ? CONTENT_URI : SHADOW_CONTENT_URI,
                    user.getIdentifier());

            final ContentResolver resolver = context.getContentResolver();
            String number = values.getAsString(NUMBER);
            long date = values.getAsLong(DATE);

            if (VERBOSE_LOG) {
                Log.i(LOG_TAG, "updateCallEntryIfExist + number " + number + ", date " + date
                        + " conference call id " + conferenceCallId);
            }

            // For VoLTE conference call, one number could be disconnect and add again. Need to
            // check the DATE here.
            String selection = NUMBER + "=? AND " + DATE + "=? AND " + CONFERENCE_CALL_ID + "=?";
            String[] selectionArgs = new String[] {number, String.valueOf(date),
                    String.valueOf(conferenceCallId)};

            Cursor cursor = null;
            try {
                cursor = resolver.query(uri, new String[] { _ID }, selection, selectionArgs, null);
                if (cursor != null && cursor.getCount() > 0) {
                    int count = resolver.update(uri, values, selection, selectionArgs);

                    cursor.moveToFirst();
                    long id = cursor.getInt(cursor.getColumnIndex(_ID));
                    return ContentUris.withAppendedId(uri, id);
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
            return null;
        }

        private static String getCurrentCountryIso(Context context) {
            String countryIso = null;
            final CountryDetector detector = (CountryDetector) context.getSystemService(
                    Context.COUNTRY_DETECTOR);
            if (detector != null) {
                final Country country = detector.detectCountry();
                if (country != null) {
                    countryIso = country.getCountryIso();
                }
            }
            return countryIso;
        }


        /**
         * If a successful call is made that is longer than this duration, update the phone number
         * in the ContactsProvider with the normalized version of the number, based on the user's
         * current country code.
         */
        private static final int MIN_DURATION_FOR_NORMALIZED_NUMBER_UPDATE_MS = 1000 * 10;

        /**
         * Save call log corresponding phone number ID.
         * @hide
         * @internal
         */
        public static final String DATA_ID = "data_id";

        /**
         * Save raw contact id of a call log corresponding to phone number.
         * @hide
         * @internal
         */
        public static final String RAW_CONTACT_ID = "raw_contact_id";

        /**
         * Save conference call id of a call log in a conference call.
         * @hide
         */
        public static final String CONFERENCE_CALL_ID = "conference_call_id";

        /**
         * The projection of calls date or conference call date.
         * @hide
         */
        public static final String SORT_DATE = "sort_date";

        /**
         * An opaque value that indicate contact store location.
         * "-1", indicates phone contacts, others, indicate sim id of a sim contact
         * @hide
         */
        public static final String CACHED_INDICATE_PHONE_SIM = "indicate_phone_or_sim_contact";

        /**
         * For SIM contact's flag, SDN's contacts value is 1,
         * ADN's contacts value is 0 card.
         * @hide
         */
        public static final String CACHED_IS_SDN_CONTACT = "is_sdn_contact";
    }

    /**
     * Columns for conference calls table.
     * @hide
     */
    public static final class ConferenceCalls implements android.provider.BaseColumns {
        private ConferenceCalls() {
        }

        /**
         * The content:// style URL for this table.
         * @hide
         */
        public static final Uri CONTENT_URI =
                Uri.parse("content://call_log/conference_calls");

        /**
         * Save group id if the conference call is started from a contacts group.
         * @hide
         */
        public static final String GROUP_ID = "group_id";

        /**
         * Save conference call date, in milliseconds since the epoch.
         * @hide
         */
        public static final String CONFERENCE_DATE = "conference_date";

        /**
         * Conference call duration in seconds.
         * @hide
         */
        public static final String CONFERENCE_DURATION = "conference_duration";

        public static synchronized Uri addConferenceCall(Context context, UserHandle user,
                long conferenceDate, int duration) {

            // append user id & path
            Uri uri = ContentProvider.maybeAddUserId(ConferenceCalls.CONTENT_URI,
                    user.getIdentifier());
            Log.i(LOG_TAG, "addConferenceCall " + uri + " date " + conferenceDate +
                    " duration " + duration);

            if (duration < 0) {
                duration = 0;
            }

            ContentValues values = new ContentValues();
            values.put(CONFERENCE_DATE, conferenceDate);
            values.put(CONFERENCE_DURATION, duration);

            Uri confUri = null;
            try {
                confUri = context.getContentResolver().insert(uri, values);
            } catch (Exception e) {
                e.printStackTrace();
            }
            Log.i(LOG_TAG, "addConferenceCall result uri " + confUri);
            return confUri;
        }

        /**
         * Update conference duration if the new value is greater.
         * @param context  The context
         * @param user     The user to update
         * @param id       Conference ID in database
         * @param duration new duration value
         */
        public static synchronized void updateConferenceDurationIfNeeded(Context context,
                UserHandle user, long id, int duration) {

            Log.i(LOG_TAG, "updateConferenceDurationIfNeeded " + user + ", id " + id +
                    " duration " + duration);

            int userId = user.getIdentifier();
            Uri uri = ContentProvider.maybeAddUserId(ConferenceCalls.CONTENT_URI, userId);
            Log.i(LOG_TAG, "Modify uri " + uri);

            Cursor cursor = null;
            ContentResolver resolver = context.getContentResolver();
            try {
                cursor = resolver.query(uri,
                        new String[] { CONFERENCE_DURATION },
                        _ID + "=?",
                        new String[] { String.valueOf(id)},
                        null);
                if (cursor != null && cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    int existingDuration = cursor.getInt(
                            cursor.getColumnIndexOrThrow(CONFERENCE_DURATION));

                    if (duration > existingDuration) {
                        Log.v(LOG_TAG, "new: " + duration + ", old: " + existingDuration);
                        ContentValues values = new ContentValues();
                        values.put(CONFERENCE_DURATION, duration);
                        resolver.update(
                                // We only support update on single ID for now
                                ContentUris.withAppendedId(uri, id),
                                values,
                                null,
                                null);
                    } else {
                        Log.v(LOG_TAG, "new: " + duration + ", old: " + existingDuration +
                                ", no need to update.");
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
    }
}

