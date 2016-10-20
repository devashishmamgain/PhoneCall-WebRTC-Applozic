package com.applozic.audiovideo.authentication;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.widget.EditText;

import applozic.com.audiovideo.R;


public class Dialog {

    public static AlertDialog createInviteDialog(String caller, DialogInterface.OnClickListener acceptClickListener, DialogInterface.OnClickListener rejectClickListener, Context context) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);

        alertDialogBuilder.setIcon(R.drawable.ic_call_black_24dp);
        alertDialogBuilder.setTitle("Conversation Invite");
        alertDialogBuilder.setMessage(caller + " invited you to conversation");
        alertDialogBuilder.setPositiveButton("Accept", acceptClickListener);
        alertDialogBuilder.setNegativeButton("Reject", rejectClickListener);
        alertDialogBuilder.setCancelable(false);

        return alertDialogBuilder.create();
    }

    public static AlertDialog createCallParticipantsDialog(EditText participantEditText, DialogInterface.OnClickListener callParticipantsClickListener, DialogInterface.OnClickListener cancelClickListener, Context context) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);

        alertDialogBuilder.setIcon(R.drawable.ic_call_black_24dp);
        alertDialogBuilder.setTitle("Invite Participant");
        alertDialogBuilder.setPositiveButton("Send", callParticipantsClickListener);
        alertDialogBuilder.setNegativeButton("Cancel", cancelClickListener);
        alertDialogBuilder.setCancelable(false);

        setParticipantFieldInDialog(participantEditText, alertDialogBuilder, context);

        return alertDialogBuilder.create();
    }

    private static void setParticipantFieldInDialog(EditText participantEditText, AlertDialog.Builder alertDialogBuilder, Context context) {
        // Add a participant field to the dialog
        participantEditText.setHint("participant name");
        int horizontalPadding = context.getResources().getDimensionPixelOffset(R.dimen.activity_horizontal_margin);
        int verticalPadding = context.getResources().getDimensionPixelOffset(R.dimen.activity_vertical_margin);
        alertDialogBuilder.setView(participantEditText, horizontalPadding, verticalPadding, horizontalPadding, 0);
    }

    public static AlertDialog createDialingDialog(String CallerName, DialogInterface.OnClickListener stopCallingListener, Context context) {

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);

        alertDialogBuilder.setTitle("Calling " + CallerName);
        alertDialogBuilder.setView(R.layout.applozic_video_inprogress);
        alertDialogBuilder.setNegativeButton("Cancel", stopCallingListener);
        alertDialogBuilder.setCancelable(false);

        return alertDialogBuilder.create();
    }

    public static AlertDialog createCloseSessionDialog(DialogInterface.OnClickListener keepSession, DialogInterface.OnClickListener closeSessionListener, Context context) {

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);

        alertDialogBuilder.setTitle("Close session ");
        alertDialogBuilder.setMessage("Do you want to close your current session?");
        alertDialogBuilder.setNegativeButton("Cancel", keepSession);
        alertDialogBuilder.setPositiveButton("Close", closeSessionListener);
        alertDialogBuilder.setCancelable(false);

        return alertDialogBuilder.create();
    }

}
