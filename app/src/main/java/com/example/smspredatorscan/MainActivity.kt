package com.example.smspredatorscan

import android.Manifest
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Try to read Cytrox domains from the assets directory
        var domainList = listOf<String>()
        try {

            val domains = assets.open("domains.txt").bufferedReader().use { it.readText() }
            domainList = domains.split("\n")

        } catch (e: IOException) {
            resultsText.setText(e.toString())
        }

        infoText.setText("Έτοιμος ο έλεγχος SMS για ${domainList.size} κακόβουλες διευθύνσεις")

        button.setOnClickListener {

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
                == PackageManager.PERMISSION_DENIED)
            {
                // Request the user to grant permission to read SMS messages
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_SMS), 2);
                resultsText.setText("Η πρόσβαση στα SMS ήταν αδύνατη")
            }

            var messageIndex = 0; //how many SMS have been read
            var numberOfHits = 0; //how many domain detections have been recorded
            var detectedDomainsSet = setOf<String>(); //which domains have been recorded

            //open the SMS inbox for one-by-one reading
            val cursor: Cursor? =
                contentResolver.query(Uri.parse("content://sms/inbox")
                    , null, null, null, null)

            if (cursor != null) {
                if (cursor.moveToFirst()) { // must check the result to prevent exception
                    do {
                        //get SMS body text
                        var SMStext = cursor.getString(cursor.getColumnIndexOrThrow("body"));
                        //check if the SMS contains any of the domains
                        val matchedDomains = domainList.filter { SMStext.contains(it, ignoreCase = true) }

                        //if the SMS contains any domain
                        if(matchedDomains.isNotEmpty())
                        {
                            numberOfHits += matchedDomains.size;
                            detectedDomainsSet = detectedDomainsSet + matchedDomains.toSet()
                        }
                        infoText.setText("Read " + messageIndex + " SMS");
                        messageIndex++;
                    } while (cursor.moveToNext())
                } else {
                    resultsText.setText("Could not read SMS")
                }
            }

            var phrasing = if(numberOfHits == 1) "εντοπισμός" else "εντοπισμοί" ;
            var results = "Αποτέλεσμα: ${numberOfHits} ${phrasing}. "

            //if there was any detection
            if(numberOfHits>0)
            {
                resultsText.setTextColor(Color.RED)
                results += "Ψάξτε τα SMS for σας για τα ακόλουθα: "
                for(domain in detectedDomainsSet)
                    results += domain + " "
            }
            else
            {
                resultsText.setTextColor(Color.GREEN)
                results += "Δεν εντοπίστηκε επίθεση με Predator."
            }

            resultsText.setText(results);

        }

    }
}