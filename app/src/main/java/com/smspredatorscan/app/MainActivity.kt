package com.smspredatorscan.app

import android.Manifest
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException

class MainActivity : AppCompatActivity() {

    var domainList = listOf<String>()

    //Waiting for the permission dialog result, called below
    val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                scanSMS()
            } else {
                notifyPermissionRejection()
            }
        }

    //Main function
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Read Cytrox domains from the assets directory
        try {

            val domains = assets.open("domains.txt")
                .bufferedReader().use { it.readText() }
            domainList = domains.split("\n")

        } catch (e: IOException) {
            resultsText.setText(e.toString())
        }

        infoText.setText("Ο έλεγχος SMS για ${domainList.size} κακόβουλες διευθύνσεις μπορεί να ξεκινήσει")

        scanBtn.setOnClickListener {

            //Check, explain and ask for SMS permission
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_SMS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    scanSMS()
                }
                ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_SMS) -> {
                    explainPermissionRequest()
            }
                else -> {
                    // Directly ask for permission
                    requestPermissionLauncher.launch(
                        Manifest.permission.READ_SMS)
                }
            }

        }

        //Load FAQ button with URL
        faqBtn.setOnClickListener {
            val url = getString(R.string.faq_url)
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            startActivity(intent)
        }

    }

    fun scanSMS()
    {
        val SMS_Scan_Thread = Thread { //run the scan on a separate thread

            //Show the progress bar
            this@MainActivity.runOnUiThread(java.lang.Runnable {
                progressBar.visibility = View.VISIBLE
            })

            var messageIndex = 0; //how many SMS have been read
            var numberOfHits = 0; //how many domain detections have been recorded
            var detectedDomainsSet = setOf<String>(); //which domains have been recorded

            //open the SMS inbox for one-by-one scanning
            val cursor: Cursor? =
                contentResolver.query(
                    Uri.parse("content://sms/inbox"), null, null, null, null
                )

            if (cursor != null) {
                if (cursor.moveToFirst()) { // must check the result to prevent exception
                    do {
                        //get SMS body text
                        var SMStext = cursor.getString(cursor.getColumnIndexOrThrow("body"));
                        //check if the SMS contains any of the domains
                        val matchedDomains =
                            domainList.filter { SMStext.contains(it, ignoreCase = true) }

                        //if the SMS contains any domain
                        if (matchedDomains.isNotEmpty()) {
                            numberOfHits += matchedDomains.size;
                            detectedDomainsSet = detectedDomainsSet + matchedDomains.toSet()
                        }
                        messageIndex++;
                    } while (cursor.moveToNext())
                } else {
                    resultsText.setText("Το SMS ${messageIndex} δεν μπόρεσε να διαβαστεί")
                }
            }

            infoText.setText("Διαβάστηκαν ${messageIndex} SMS");

            var phrasing = if(numberOfHits == 1) "εντοπισμός" else "εντοπισμοί" ;
            var results = "Αποτέλεσμα: ${numberOfHits} ${phrasing}. "

            //If there was any detection
            if(numberOfHits>0)
            {
                resultsText.setTextColor(Color.RED)
                results += "Ψάξτε τα SMS σας για τα ακόλουθα: "
                for(domain in detectedDomainsSet)
                    results += domain + " "
            }
            else
            {
                resultsText.setTextColor(Color.GREEN)
                results += "Δεν εντοπίστηκε SMS επίθεσης, αλλά δεν μπορεί να εξασφαλιστεί ότι δεν υπήρξε."
            }

            //Show results
            resultsText.setText(results);

            //Hide the progress bar
            this@MainActivity.runOnUiThread(java.lang.Runnable {
                progressBar.visibility = View.GONE
            })

        }
        SMS_Scan_Thread.start()

        //Show the explanation button and load it with the corresponding URL
        explainBtn.setVisibility(View.VISIBLE)
        explainBtn.setOnClickListener {
            val url =
                if(resultsText.getText().toString().contains(" 0"))
                    getString(R.string.no_detection_url)
                else
                    getString(R.string.detection_url)
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            startActivity(intent)
        }
    }

    //Pop-up in case the user rejected SMS permission request
    fun notifyPermissionRejection()
    {
        val alertDialog: AlertDialog? = this?.let {
            val builder = AlertDialog.Builder(it)
            builder.apply {
                setMessage(R.string.permission_rejection_notification)

                setTitle("Ειδοποίηση")

                setPositiveButton("ΚΛΕΙΣΙΜΟ",
                    DialogInterface.OnClickListener { dialog, id ->
                        dialog.cancel()
                    })
                setNegativeButton("ΜΕΤΑΒΑΣΗ ΣΤΙΣ ΡΥΘΜΙΣΕΙΣ",
                    DialogInterface.OnClickListener { dialog, id ->
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        val uri = Uri.fromParts("package", packageName, null)
                        intent.data = uri
                        startActivity(intent)
                    })
                setNeutralButton("ΕΙΝΑΙ ΑΣΦΑΛΕΣ;",
                    DialogInterface.OnClickListener { dialog, id ->
                        val url = getString(R.string.safety_url)
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.data = Uri.parse(url)
                        startActivity(intent)
                    })
            }

            builder.create()
            builder.show()
        }

    }

    //Pop-up to explain SMS permission request
    fun explainPermissionRequest()
    {
        val alertDialog: AlertDialog? = this?.let {
            val builder = AlertDialog.Builder(it)
            builder.apply {
                setMessage(R.string.permission_explanation)

                setTitle("Ειδοποίηση")

                setPositiveButton("ΟΚ",
                    DialogInterface.OnClickListener { dialog, id ->
                        requestPermissionLauncher.launch(
                            Manifest.permission.READ_SMS)
                    })
                setNeutralButton("ΠΕΡΙΣΣΟΤΕΡΑ",
                    DialogInterface.OnClickListener { dialog, id ->
                        val url = getString(R.string.safety_url)
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.data = Uri.parse(url)
                        startActivity(intent)
                    })
            }

            builder.create()
            builder.show()
        }
    }

}