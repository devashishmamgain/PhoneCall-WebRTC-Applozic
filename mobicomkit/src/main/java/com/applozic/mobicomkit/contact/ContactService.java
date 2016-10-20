package com.applozic.mobicomkit.contact;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;

import com.applozic.mobicomkit.api.attachment.FileClientService;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by devashish on 27/12/14.
 */
public class ContactService {

    private Context context;

    public ContactService(Context context) {
        this.context = context;
    }

    /**
     * @param contactData
     * @return
     */
    public File vCard(Uri contactData) throws Exception {
        Cursor cursor = context.getContentResolver().query(contactData, null, null, null, null);
        cursor.moveToFirst();
        String lookupKey = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
        Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_VCARD_URI, lookupKey);
        String name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));

        AssetFileDescriptor fd;

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "CONTACT_" + timeStamp + "_" + ".vcf";

        File outputFile = FileClientService.getFilePath(imageFileName, context, "text/x-vcard");

        fd = context.getContentResolver().openAssetFileDescriptor(uri, "r");

        FileInputStream fis = fd.createInputStream();
        byte[] buf = new byte[(int) fd.getDeclaredLength()];
        fis.read(buf);
        String cvFdata = new String(buf);
        if (!MobiComVCFParser.validateData(cvFdata)) {
            Log.i("vCard ::", cvFdata.toString());
            throw new Exception("contact exported is not proper in proper format");
        }
        Log.i(" data:", new String(buf));
        FileOutputStream fileOutputStream = new FileOutputStream(outputFile.getAbsoluteFile());
        fileOutputStream.write(buf);
        fileOutputStream.close();
        return outputFile;
    }


}
