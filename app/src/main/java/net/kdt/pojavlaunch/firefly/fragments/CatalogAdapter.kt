package net.kdt.pojavlaunch.firefly.fragments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.kdt.pojavlaunch.firefly.R

class CatalogAdapter(
    private val onInstall: (ModManagerFragment.CatalogEntryUi) -> Unit
) : RecyclerView.Adapter<CatalogAdapter.VH>() {

    private var items: List<ModManagerFragment.CatalogEntryUi> = emptyList()

    fun submit(list: List<ModManagerFragment.CatalogEntryUi>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.view_mod_catalog_item, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val e = items[position]
        holder.name.text = e.title
        holder.meta.text = buildString {
            if (e.description.isNotBlank()) append(e.description.take(90))
            append("  ·  ")
            append(formatDownloads(e.downloads))
        }
        holder.install.setOnClickListener { onInstall(e) }
    }

    private fun formatDownloads(d: Long): String =
        if (d >= 1_000_000) "${d / 1_000_000}M unduhan" else if (d >= 1_000) "${d / 1_000}K unduhan" else "$d unduhan"

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.catalog_item_name)
        val meta: TextView = v.findViewById(R.id.catalog_item_meta)
        val install: View = v.findViewById(R.id.catalog_item_install)
    }
}
