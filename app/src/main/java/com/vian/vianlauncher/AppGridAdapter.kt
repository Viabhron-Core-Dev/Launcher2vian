package com.vian.vianlauncher

import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.BitmapDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppGridAdapter(
    private var allApps: List<ResolveInfo>,
    private val pm: PackageManager,
    private val onAppClicked: (ResolveInfo) -> Unit,
    private val onAppLongClicked: (ResolveInfo) -> Unit = {}
) : RecyclerView.Adapter<AppGridAdapter.AppViewHolder>() {

    private var displayApps: List<ResolveInfo> = allApps
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    class AppViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.iv_app_icon)
        val label: TextView = view.findViewById(R.id.tv_app_label)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app_drawer, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val resolveInfo = displayApps[position]
        holder.label.text = resolveInfo.loadLabel(pm)
        holder.icon.setImageResource(android.R.drawable.sym_def_app_icon)
        
        holder.itemView.setOnClickListener {
            onAppClicked(resolveInfo)
        }

        holder.itemView.setOnLongClickListener {
            onAppLongClicked(resolveInfo)
            true
        }

        coroutineScope.launch {
            val drawable = withContext(Dispatchers.IO) {
                resolveInfo.loadIcon(pm)
            }
            holder.icon.setImageDrawable(drawable)
        }
    }

    override fun getItemCount(): Int = displayApps.size

    fun filter(query: String) {
        val lowerQuery = query.lowercase()
        displayApps = if (query.isEmpty()) {
            allApps
        } else {
            allApps.filter { it.loadLabel(pm).toString().lowercase().contains(lowerQuery) }
        }
        notifyDataSetChanged()
    }

    fun updateApps(newApps: List<ResolveInfo>) {
        allApps = newApps
        displayApps = newApps
        notifyDataSetChanged()
    }
}
