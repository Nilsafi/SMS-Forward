package com.enixcoda.smsforward;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.telephony.SmsMessage;

import androidx.preference.PreferenceManager;

public class SMSReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!intent.getAction().equals(android.provider.Telephony.Sms.Intents.SMS_RECEIVED_ACTION))
            return;

        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        final boolean enableSMS = sharedPreferences.getBoolean(context.getString(R.string.key_enable_sms), false);
        final String targetNumber = sharedPreferences.getString(context.getString(R.string.key_target_sms), "");

        final boolean enableWeb = sharedPreferences.getBoolean(context.getString(R.string.key_enable_web), false);
        final String targetWeb = sharedPreferences.getString(context.getString(R.string.key_target_web), "");

        final boolean enableTelegram = sharedPreferences.getBoolean(context.getString(R.string.key_enable_telegram), false);
        final String targetTelegram = sharedPreferences.getString(context.getString(R.string.key_target_telegram), "");
        final String telegramToken = sharedPreferences.getString(context.getString(R.string.key_telegram_apikey), "");

        if (!enableSMS && !enableTelegram && !enableWeb) return;

        final Bundle bundle = intent.getExtras();
        final Object[] pduObjects = (Object[]) bundle.get("pdus");
        if (pduObjects == null) return;

        for (Object messageObj : pduObjects) {
            SmsMessage currentMessage = SmsMessage.createFromPdu((byte[]) messageObj, (String) bundle.get("format"));
            String senderNumber = currentMessage.getDisplayOriginatingAddress();
            String rawMessageContent = currentMessage.getDisplayMessageBody();

            if (senderNumber.equals(targetNumber)) {
                // reverse message
                String formatRegex = "To (\\+?\\d+?):\\n((.|\\n)*)";
                if (rawMessageContent.equals(formatRegex)) {
                    String forwardNumber = rawMessageContent.replaceFirst(formatRegex, "$1");
                    String forwardContent = rawMessageContent.replaceFirst(formatRegex, "$2");
                    Forwarder.sendSMS(forwardNumber, forwardContent);
                }
            } else {
                // normal message, forwarded
                if (enableSMS && !targetNumber.equals(""))
                    Forwarder.forwardViaSMS(senderNumber, rawMessageContent, targetNumber);
                if (enableTelegram && !targetTelegram.equals("") && !telegramToken.equals(""))
                    Forwarder.forwardViaTelegram(senderNumber, rawMessageContent, targetTelegram, telegramToken);
                if (enableWeb && !targetWeb.equals(""))
                    Forwarder.forwardViaWeb(senderNumber, rawMessageContent, targetWeb);
            }
        }
    }
}
