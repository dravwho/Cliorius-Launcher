package net.kdt.pojavlaunch.firefly.fragments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView
import com.movtery.feature.mod.parser.ModInfo
import net.kdt.pojavlaunch.firefly.R
import java.io.File

class ModsAdapter(
    private val onToggle: (ModInfo, Boolean) -> Unit,
    private val onDelete: (ModInfo) -> Unit
) : RecyclerView.Adapter<ModsAdapter.VH>() {

    private var items: List<ModInfo> = emptyList()

    fun submit(list: List<ModInfo>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.view_mod_item, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val info = items[position]
        holder.name.text = info.name.ifBlank { info.id }
        val file = info.file
        val enabled = file != null && file.name.endsWith(".jar", ignoreCase = true)
        holder.meta.text = buildString {
            append(info.id)
            if (info.version.isNotBlank()) append(" · v").append(info.version)
            if (!enabled) append(" · (nonaktif)")
        }
        holder.toggle.setOnCheckedChangeListener(null)
        holder.toggle.isChecked = enabled
        holder.toggle.setOnCheckedChangeListener { _, checked -> onToggle(info, checked) }
        holder.delete.setOnClickListener { onDelete(info) }
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.mod_item_name)
        val meta: TextView = v.findViewById(R.id.mod_item_meta)
        val toggle: SwitchCompat = v.findViewById(R.id.mod_item_toggle)
        val delete: ImageButton = v.findViewById(R.id.mod_item_delete)
    }
}
