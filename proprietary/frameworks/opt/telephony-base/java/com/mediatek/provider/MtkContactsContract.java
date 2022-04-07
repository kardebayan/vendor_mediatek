/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
package com.mediatek.provider;

import android.accounts.Account;
import android.annotation.SdkConstant;
import android.annotation.SdkConstant.SdkConstantType;
import android.annotation.SystemApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.CursorEntityIterator;
import android.content.Entity;
import android.content.EntityIterator;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.database.DatabaseUtils;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.view.View;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class MtkContactsContract {
    public interface ContactsColumns {
        /**
         * An opaque value that indicate contact store location.
         * "-1", indicates phone contacts
         * others, indicate sim id of a sim contact
         *
         * @hide
         */

        public static final String INDICATE_PHONE_SIM = "indicate_phone_or_sim_contact";

        /**
         * For a SIM/USIM contact, this value is its index in the relative SIM
         * card.
         *
         * @hide
         */
        public static final String INDEX_IN_SIM = "index_in_sim";

        /**
         * Whether the contact should always be sent to voicemail for VT. If
         * missing, defaults to false.
         * <P>
         * Type: INTEGER (0 for false, 1 for true)
         * </P>
         *
         * @hide
         */
        public static final String SEND_TO_VOICEMAIL_VT = "send_to_voicemail_vt";

        /**
         * Whether the contact should always be sent to voicemail for SIP. If
         * missing, defaults to false.
         * <P>
         * Type: INTEGER (0 for false, 1 for true)
         * </P>
         *
         * @hide
         */
        public static final String SEND_TO_VOICEMAIL_SIP = "send_to_voicemail_sip";

        /**
         * To filter the Contact for Widget.
         *
         * @hide
         */
        public static final String FILTER = "filter";
        /**
         * To filter the Contact for Widget.
         *
         * @hide
         */
        public static final int FILTER_NONE = 0;
        /**
         * To filter the Contact for Widget.
         *
         * @hide
         */
        public static final int FILTER_WIDGET = 1;

        /**
         * For SIM contact's flag, SDN's contacts value is 1, ADN's contacts value is 0
         * card.
         *
         * @hide
         */
        public static final String IS_SDN_CONTACT = "is_sdn_contact";
    }
    
    public interface RawContactsColumns {
        /**
         * An opaque value that indicate contact store location.
         * "-1", indicates phone contacts
         * others, indicate sim id of a sim contact
         *
         * @hide
         */
        public static final String INDICATE_PHONE_SIM = "indicate_phone_or_sim_contact";

        /**
         * For a SIM/USIM contact, this value is its index in the relative SIM
         * card.
         *
         * @hide
         */
        public static final String INDEX_IN_SIM = "index_in_sim";

        /**
         * Whether the contact should always be sent to voicemail for VT. If
         * missing, defaults to false.
         * <P>
         * Type: INTEGER (0 for false, 1 for true)
         * </P>
         *
         * @hide
         */
        public static final String SEND_TO_VOICEMAIL_VT = "send_to_voicemail_vt";

        /**
         * Whether the contact should always be sent to voicemail for SIP. If
         * missing, defaults to false.
         * <P>
         * Type: INTEGER (0 for false, 1 for true)
         * </P>
         *
         * @hide
         */
        public static final String SEND_TO_VOICEMAIL_SIP = "send_to_voicemail_sip";

        /**
         * M:
         * For SIM contact's flag, SDN's contacts value is 1, ADN's contacts value is 0
         * card.
         *
         * @hide
         */
        public static final String IS_SDN_CONTACT = "is_sdn_contact";
    }
    
    public static final class RawContacts  {
        /**
         * Indicate flag: Indicate it is a phone contact.
         * @hide
         * @internal
         */
        public static final int INDICATE_PHONE = -1;
        /**
         * time stamp that is updated whenever version changes.
         * <P>
         * Type: INTEGER
         * </P>
         *
         * @hide
         * @internal
         */
        public static final String TIMESTAMP = "timestamp";
    }
    
    public interface DataColumns {
        /**
         * M: Code add by Mediatek inc.
         * @hide
         */
        public static final String IS_ADDITIONAL_NUMBER = "is_additional_number";

    }
    
    public static final class CommonDataKinds {
        public static final class Phone {
            /**
             * M: Add for AAS, call this API to get common or AAS label.
             * @param context context
             * @param type phone type
             * @param label label
             * @return AAS label if type is PHONE_TYPE_AAS.
             *
             * @hide
             */
            public static final CharSequence getTypeLabel(Context context, int type,
                    CharSequence label) {
                if (type == Aas.PHONE_TYPE_EMPTY) {
                    return Aas.LABEL_EMPTY;
                } else if (type == Aas.PHONE_TYPE_AAS) {
                    if (!TextUtils.isEmpty(label)) {
                        return Aas.getLabel(context.getContentResolver(), label);
                    } else {
                        return Aas.LABEL_EMPTY;
                    }
                } else {
                    return ContactsContract.CommonDataKinds.Phone.getTypeLabel(context.getResources(), type, label);
                }
            }
        }
        
        /**
         * M: used to support VOLTE IMS Call feature.
         *
         * @hide
         */
        public static final class ImsCall {
            /**
             * This utility class cannot be instantiated.
             */
            private ImsCall() {}

            /**
             * MIME type used when storing this in data table.
             * @internal
             */
            public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/ims";
            
            /**
             * The data for the contact method.
             * <P>Type: TEXT</P>
             */
            public static final String DATA = "data1";
            
            /**
             * The type of data, for example Home or Work.
             * <P>Type: INTEGER</P>
             */
            public static final String TYPE = "data2";
            
            /**
             * The user defined label for the contact method.
             * <P>Type: TEXT</P>
             */
            public static final String LABEL = "data3";

            /**
             * The ImsCall URL string.
             * <P>Type: TEXT</P>
             * @internal
             */
            public static final String URL = DATA;
        }
    }
    
    public static final class Groups {
        /**
         * M:
         * Used with ContactsContract.Contacts.CONTENT_GROUP_URI to query by group ID.
         *
         * @hide
         */
        public static final String QUERY_WITH_GROUP_ID = "query_with_group_id";
    }
    
    public static final class ProviderStatus {
        /**
         * M:
         * The content:// style URI for this table. Requests to this URI can be
         * performed on the UI thread because they are always unblocking.
         *
         * @hide
         */
        public static final Uri SIM_CONTACT_CONTENT_URI = Uri.withAppendedPath(
                ContactsContract.AUTHORITY_URI, "provider_sim_contact_status");
    }
    
    public static final class DataUsageFeedback {
        /**
         * M:
         * An integer representing the current sim contact status of the provider.
         * @hide
         */
        public static final String SIM_CONTACT_STATUS = "sim_contact_status";

        /**
         * M:
         * Default sim contact status of the provider.
         * @hide
         */
        public static final int SIM_CONTACT_STATUS_NORMAL = 0;

        /**
         * M:
         * The status used when the provider is in the process of SIM contacts loading.  Contacts
         * are temporarily unaccessible.
         * @hide
         */
        public static final int SIM_CONTACT_STATUS_LOADING = 1;
    }
    
    public static final class Intents {
        public static final class Insert {
            /**
             * M:
             * The extra field for the sip address flag.
             * <P>Type: boolean</P>
             * @hide
             * @internal
             */
           public static final String SIP_ADDRESS = "sip_address";

           /**
            * M:
            * The extra field for the ims address flag.
            * <P>Type: String</P>
            * @hide
            * @internal
            */
           public static final String IMS_ADDRESS = "ims_address";
        }
    }
    /**
     * Columns for dialer search displayed information.
     *
     * @hide
     */
    protected interface DialerSearchColumns {
        public static final String NAME_LOOKUP_ID = "_id";
        public static final String CALL_LOG_ID = "call_log_id";
        public static final String CONTACT_ID = "contact_id";
        public static final String NAME = "name";
        public static final String PHOTO_ID = "photo_id";
        public static final String CALL_TYPE = "call_type";
        public static final String PHONE_ACCOUNT_ID = "phone_account_id";
        public static final String PHONE_ACCOUNT_COMPONENT_NAME = "phone_account_component_name";
        public static final String CALL_NUMBER = "call_number";
        public static final String CALL_PHONE_TYPE = "call_phone_type";
        public static final String SEARCH_PHONE_NUMBER = "search_phone_number";
        public static final String SEARCH_PHONE_TYPE = "search_phone_type";
        public static final String SEARCH_PHONE_LABEL = "search_phone_label";
        public static final String FIRST_PHONE_NUMBER = "first_phone_number";
        public static final String FIRST_PHONE_TYPE = "first_phone_type";
        public static final String CALL_DATE = "call_date";
        public static final String NORMALIZED_NAME = "normalized_name";
        public static final String NAME_TYPE = "name_type";
        public static final String NUMBER_COUNT = "number_count";
        public static final String CONTACT_NAME_LOOKUP = "contact_name_lookup";
        public static final String CONTACT_STARRED = "contact_starred";
        public static final String INDICATE_PHONE_SIM = "indicate_phone_sim";
        public static final String SORT_KEY_PRIMARY = "sort_key";
        public static final String SEARCH_PHONE_DATA_ID = "search_phone_data_id";
        public static final String SEARCH_DATA_OFFSETS = "search_data_offsets";
        public static final String NUMBER_PRESENTATION = "number_presentation";
    }
    
    public static final class PhoneLookup implements ContactsColumns {
        
    }

    /**
     * View dialer Search columns.
     * @hide
     */
    protected interface ViewDialerSearchColumns {
        public static final String NAME_LOOKUP_ID = "_id";
        public static final String CONTACT_ID = "vds_contact_id";
        public static final String RAW_CONTACT_ID = "vds_raw_contact_id";
        public static final String NAME = "vds_name";
        public static final String NUMBER_COUNT = "vds_number_count";
        public static final String CALL_LOG_ID = "vds_call_log_id";
        public static final String CALL_TYPE = "vds_call_type";
        public static final String CALL_DATE = "vds_call_date";
        public static final String CALL_GEOCODED_LOCATION = "vds_geocoded_location";
        public static final String SIM_ID = "vds_sim_id";
        public static final String VTCALL = "vds_vtcall";
        public static final String SEARCH_PHONE_NUMBER = "vds_phone_number";
        public static final String SEARCH_PHONE_TYPE = "vds_phone_type";
        public static final String CONTACT_NAME_LOOKUP = "vds_lookup";
        public static final String PHOTO_ID = "vds_photo_id";
        public static final String CONTACT_STARRED = "vds_starred";
        public static final String INDICATE_PHONE_SIM = "vds_indicate_phone_sim";
        public static final String IS_SDN_CONTACT = "vds_is_sdn_contact";
        public static final String SORT_KEY_PRIMARY = "vds_sort_key";
        public static final String SORT_KEY_ALTERNATIVE = "vds_sort_key_alternative";
        public static final String SEARCH_DATA_OFFSETS = "search_data_offsets";
        public static final String NAME_ALTERNATIVE = "vds_name_alternative";
        public static final String SEARCH_DATA_OFFSETS_ALTERNATIVE =
                                       "search_data_offsets_alternative";
        public static final String NAME_ID = "vds_name_id";
        public static final String NUMBER_ID = "vds_number_id";
        public static final String DS_DATA1 = "vds_data1";
        public static final String DS_DATA2 = "vds_data2";
        public static final String DS_DATA3 = "vds_data3";
        // substitute phone_account_id,phone_account_component_name for KK sim_id
        public static final String PHONE_ACCOUNT_ID = "vds_phone_account_id";
        public static final String PHONE_ACCOUNT_COMPONENT_NAME =
                                       "vds_phone_account_component_name";
        // add presentation field for read number presentation from dialer
        public static final String NUMBER_PRESENTATION = "vds_number_presentation";
        // add search_phone_label field for read number label information from dialer
        public static final String SEARCH_PHONE_LABEL = "vds_search_phone_label";
    }

    /**
     * Columns for dialer search displayed information.
     *
     * @hide
     */
    public static final class DialerSearch implements android.provider.BaseColumns, ViewDialerSearchColumns {


        /**
         * The index for highlight number.
         * @hide
         * @internal
         */
        public static final String MATCHED_DATA_OFFSET = "matched_data_offset"; //For results

        /**
         * The index for highlight name.
         * @hide
         * @internal
         */
        public static final String MATCHED_NAME_OFFSET = "matched_name_offset";
        private DialerSearch() {
        }
    }

    /**
     * Class definded for AAS (Additional number Alpha String).
     * @hide
     */
    public static final class Aas {

        /**
         * @internal
         */
        public static final int PHONE_TYPE_AAS = 101;

        /**
         * Type for primary number which has no phone type.
         * @internal
         */
        public static final int PHONE_TYPE_EMPTY = 102;

        /**
         * Label for empty type.
         * @internal
         */
        public static final String LABEL_EMPTY = "";

        /**
         * The method to get AAS.
         * @internal
         */
        public static final String AAS_METHOD = "get_aas";

        /**
         * The key to retrieve from the returned Bundle to obtain the AAS label.
         * @internal
         */
        public static final String KEY_AAS = "aas";

        /**
         * The symbol to separate sub id and index when build AAS indicator.
         * @internal
         */
        public static final String ENCODE_SYMBOL = "-";

        /**
         * The function to build AAS indicator.
         * ex. subId is 1 and index is 2 then the indicator will be "1-2".
         * @param subId sub ID
         * @param indexInSim index in SIM
         * @return indicator
         * @internal
         */
        public static final String buildIndicator(int subId, int indexInSim) {
            return new StringBuffer().append(subId).append(ENCODE_SYMBOL).append(indexInSim)
                    .toString();
        }

        /**
         * The function to get AAS, return empty string if not exsit.
         * @param resolver content resolver
         * @param indicator it contains sub id and aas index info.
         * @return return aas label
         * @internal
         */
        public static CharSequence getLabel(ContentResolver resolver, CharSequence indicator) {
            Bundle response = resolver.call(ContactsContract.AUTHORITY_URI, AAS_METHOD,
                    indicator.toString(), null);
            if (response != null) {
                return response.getCharSequence(KEY_AAS, "");
            }
            return "";
        }
    }
}
