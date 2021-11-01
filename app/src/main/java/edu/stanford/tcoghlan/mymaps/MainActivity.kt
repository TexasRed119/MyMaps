package edu.stanford.tcoghlan.mymaps

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import edu.stanford.tcoghlan.mymaps.models.Place
import edu.stanford.tcoghlan.mymaps.models.UserMap
import java.io.*

const val EXTRA_USER_MAP = "EXTRA_USER_MAP"
private const val TAG = "MainActivity"
private const val FILENAME = "UserMaps.data"
private const val REQUEST_CODE = 1234
const val EXTRA_MAP_TITLE = "EXTRA_MAP_TITLE"
class MainActivity : AppCompatActivity() {
    private lateinit var rvMaps: RecyclerView
    private lateinit var fabCreateMap: FloatingActionButton
    private lateinit var userMaps: MutableList<UserMap>
    private lateinit var mapAdapter: MapsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        rvMaps = findViewById<RecyclerView>(R.id.rvMaps)
        fabCreateMap = findViewById<FloatingActionButton>(R.id.fabCreateMap)

        val userMapsFromFile = deserializeUserMaps(this)
        userMaps = generateSampleData().toMutableList()
        userMaps.addAll(userMapsFromFile)

        //set layout manager on recycler view
        rvMaps.layoutManager = LinearLayoutManager(this)
        //set adapter on recycler view
        mapAdapter = MapsAdapter(this, userMaps, object: MapsAdapter.OnClickListener {
            override fun onItemClick(position: Int) {
                Log.i(TAG, "onItemClick $position")
                //when user taps on view in RV, navigate to new activity
                val intent = Intent(this@MainActivity, DisplayMapActivity::class.java)
                intent.putExtra(EXTRA_USER_MAP, userMaps[position])
                startActivity(intent)
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
            }
        })
        rvMaps.adapter = mapAdapter

        fabCreateMap.setOnClickListener {
            Log.i(TAG, "Tap on FAB")
            showAlertDialog()
        }
    }

    private fun showAlertDialog() {
        val mapFormView = LayoutInflater.from(this).inflate(R.layout.dialog_create_map, null)
        val dialog =
            AlertDialog.Builder(this)
                .setTitle("Map Title")
                .setView(mapFormView)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("OK", null)
                .show()

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener{
            val title = mapFormView.findViewById<EditText>(R.id.etTitle).text.toString()
            if (title.trim().isEmpty()){
                Toast.makeText(this, "Map must have non-empty title", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            //navigate to create map activity
            val intent = Intent(this@MainActivity, CreateMapActivity::class.java)
            intent.putExtra(EXTRA_MAP_TITLE, title)
            startActivityForResult(intent, REQUEST_CODE)
            dialog.dismiss()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE && requestCode == Activity.RESULT_OK){
            //get new map data
            val userMap = data?.getSerializableExtra(EXTRA_USER_MAP) as UserMap
            Log.i(TAG, "onActivityResult with new map title ${userMap.title}")
            userMaps.add(userMap)
            mapAdapter.notifyItemInserted(userMaps.size - 1)
            serializeUserMaps(this, userMaps)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun serializeUserMaps(context: Context, userMaps: List<UserMap>) {
        Log.i(TAG, "serializeUserMaps")
        ObjectOutputStream(FileOutputStream(getDataFile(context))).use { it.writeObject(userMaps) }
    }

    private fun deserializeUserMaps(context: Context) : List<UserMap> {
        Log.i(TAG, "deserializeUserMaps")
        val dataFile = getDataFile(context)
        if (!dataFile.exists()){
            Log.i(TAG, "Data file does not yet exist")
            return emptyList()
        }
        ObjectInputStream(FileInputStream(dataFile)).use { return it.readObject() as List<UserMap> }
    }

    private fun getDataFile(context: Context): File {
        Log.i(TAG, "getting file from directory ${context.filesDir}")
        return File(context.filesDir, FILENAME)
    }

    private fun generateSampleData(): List<UserMap> {
        return listOf(
            UserMap(
                "Memories from University",
                listOf(
                    Place("Branner Hall", "Best dorm at Stanford", 37.426, -122.163),
                    Place("Gates CS building", "Many long nights in this basement", 37.430, -122.173),
                    Place("Pinkberry", "First date with my wife", 37.444, -122.170)
                )
            ),
            UserMap("January vacation planning!",
                listOf(
                    Place("Tokyo", "Overnight layover", 35.67, 139.65),
                    Place("Ranchi", "Family visit + wedding!", 23.34, 85.31),
                    Place("Singapore", "Inspired by \"Crazy Rich Asians\"", 1.35, 103.82)
                ))
            )
    }
}