package org.kaqui.settings

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.Toast
import kotlinx.android.synthetic.main.jlpt_selection_activity.*
import org.kaqui.R
import org.kaqui.StatsFragment
import org.kaqui.model.KaquiDb

class JlptSelectionActivity : AppCompatActivity() {
    private lateinit var db: KaquiDb
    private lateinit var statsFragment: StatsFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.jlpt_selection_activity)

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        statsFragment = StatsFragment.newInstance()
        statsFragment.setShowDisabled(true)
        supportFragmentManager.beginTransaction()
                .replace(R.id.global_stats, statsFragment)
                .commit()

        db = KaquiDb.getInstance(this)

        jlpt_selection_list.adapter = JlptLevelSelectionAdapter(this)
        jlpt_selection_list.onItemClickListener = AdapterView.OnItemClickListener(this::onListItemClick)
    }

    override fun onStart() {
        super.onStart()

        statsFragment.updateStats(db.kanjiView)
        (jlpt_selection_list.adapter as JlptLevelSelectionAdapter).notifyDataSetChanged()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.jlpt_selection_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.search -> {
                startActivity(Intent(this, KanjiSearchActivity::class.java))
                true
            }
            R.id.import_kanji_selection -> {
                importKanjis()
                true
            }
            else ->
                super.onOptionsItemSelected(item)
        }
    }

    private fun onListItemClick(l: AdapterView<*>, v: View, position: Int, id: Long) {
        val item = l.adapter.getItem(position) as Map<String, Any>
        val level = item["level"] as Int

        startActivity(Intent(this, KanjiSelectionActivity::class.java).putExtra("level", level))
    }

    private fun importKanjis() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)
                return
            }
        }

        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "file/*"
        startActivityForResult(intent, PICK_IMPORT_FILE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (grantResults.all({ it == PackageManager.PERMISSION_GRANTED }))
            importKanjis()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != PICK_IMPORT_FILE)
            return super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != Activity.RESULT_OK || data == null)
            return

        try {
            val kanjis = contentResolver.openInputStream(data.data).bufferedReader().readText()
            db.setSelection(kanjis)
        } catch (e: Exception) {
            Log.e(TAG, "Could not import file", e)
            Toast.makeText(this, "Could not import file: " + e.toString(), Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        private const val TAG = "JlptSelectionActivity"

        private const val PICK_IMPORT_FILE = 1
    }
}