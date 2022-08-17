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

        var domainList = listOf<String>()
        try {

            val domains = assets.open("domains.txt").bufferedReader().use { it.readText() }
            domainList = domains.split("\n")

        } catch (e: IOException) {
            resultsText.setText(e.toString())
        }

        infoText.setText("Ready to check your SMS for ${domainList.size} domains")

        button.setOnClickListener {

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_DENIED) {
                // Request the user to grant permission to read SMS messages
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_SMS), 2);
                resultsText.setText("Permission to read SMS denied")
            }

            var messageIndex = 0;
            var numberOfHits = 0;
            var detectedDomainsSet = setOf<String>();

            val cursor: Cursor? =
                contentResolver.query(Uri.parse("content://sms/inbox"), null, null, null, null)

            if (cursor != null) {
                if (cursor.moveToFirst()) { // must check the result to prevent exception
                    do {
                        var SMStext = cursor.getString(cursor.getColumnIndexOrThrow("body"));
                        val matchedDomains = domainList.filter { SMStext.contains(it, ignoreCase = true) }

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

            var phrasing = if(numberOfHits == 1) "hit" else "hits" ;
            var results = "Found ${numberOfHits} ${phrasing}. "
            if(numberOfHits>0)
            {
                resultsText.setTextColor(Color.RED)
                results += "Search your SMS for the following: "
                for(domain in detectedDomainsSet)
                    results += domain + " "
            }
            else
            {
                resultsText.setTextColor(Color.GREEN)
                results += "No attack detected."
            }

            resultsText.setText(results);

        }

    }
}