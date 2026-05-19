package com.aboneliklerim.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class TagsActivity : BaseActivity() {

    private lateinit var etTagName: EditText
    private lateinit var rvTags: RecyclerView
    private lateinit var tvEmptyHint: View
    private var tagsList = mutableListOf<String>()
    private lateinit var adapter: TagsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tags)

        loadTags()

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        etTagName = findViewById(R.id.etTagName)
        rvTags = findViewById(R.id.rvTags)
        tvEmptyHint = findViewById(R.id.tvEmptyTagsHint)

        rvTags.layoutManager = LinearLayoutManager(this)
        adapter = TagsAdapter(tagsList, { selectedTag ->
            val resultIntent = Intent()
            resultIntent.putExtra("selected_tag", selectedTag)
            setResult(RESULT_OK, resultIntent)
            finish()
        }, { tagToDelete ->
            tagsList.remove(tagToDelete)
            saveTags()
            updateUI()
        })
        rvTags.adapter = adapter

        findViewById<ImageButton>(R.id.btnAddTag).setOnClickListener {
            val name = etTagName.text.toString().trim()
            if (name.isNotEmpty()) {
                if (!tagsList.contains(name)) {
                    tagsList.add(0, name)
                    saveTags()
                }
                val resultIntent = Intent()
                resultIntent.putExtra("selected_tag", name)
                setResult(RESULT_OK, resultIntent)
                finish()
            }
        }

        updateUI()
    }

    private fun updateUI() {
        if (tagsList.isEmpty()) {
            tvEmptyHint.visibility = View.VISIBLE
            rvTags.visibility = View.GONE
        } else {
            tvEmptyHint.visibility = View.GONE
            rvTags.visibility = View.VISIBLE
        }
        adapter.notifyDataSetChanged()
    }

    private fun saveTags() {
        val prefs = getSharedPreferences("AboneliklerimData", Context.MODE_PRIVATE)
        prefs.edit().putString("saved_tags", Gson().toJson(tagsList)).apply()
    }

    private fun loadTags() {
        val prefs = getSharedPreferences("AboneliklerimData", Context.MODE_PRIVATE)
        val json = prefs.getString("saved_tags", null)
        if (json != null) {
            val type = object : TypeToken<MutableList<String>>() {}.type
            tagsList = Gson().fromJson(json, type)
        } else {
            // Default tags
            tagsList = mutableListOf(
                getString(R.string.tag_entertainment),
                getString(R.string.tag_education),
                getString(R.string.tag_music),
                getString(R.string.tag_gaming),
                getString(R.string.tag_work),
                getString(R.string.tag_other)
            )
        }
    }

    inner class TagsAdapter(
        private val items: List<String>,
        private val onSelect: (String) -> Unit,
        private val onDelete: (String) -> Unit
    ) : RecyclerView.Adapter<TagsAdapter.TVH>() {
        
        inner class TVH(v: View) : RecyclerView.ViewHolder(v) {
            val tvName: TextView = v.findViewById(R.id.tvTagName)
            val btnDelete: ImageButton = v.findViewById(R.id.btnDeleteTag)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TVH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_tag, parent, false)
            return TVH(v)
        }

        override fun onBindViewHolder(holder: TVH, position: Int) {
            val tag = items[position]
            holder.tvName.text = tag
            holder.itemView.setOnClickListener { onSelect(tag) }
            holder.btnDelete.setOnClickListener { onDelete(tag) }
        }

        override fun getItemCount() = items.size
    }
}
