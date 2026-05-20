package com.taojing.androidtest.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.taojing.androidtest.R

class DeviceListAdapter(
    private val onItemClick: (DeviceListItem.Device) -> Unit,
    private val onItemLongClick: (DeviceListItem.Device) -> Unit,
) : ListAdapter<DeviceListItem, RecyclerView.ViewHolder>(DIFF) {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_DEVICE = 1

        private val DIFF = object : DiffUtil.ItemCallback<DeviceListItem>() {
            override fun areItemsTheSame(old: DeviceListItem, new: DeviceListItem): Boolean {
                return when {
                    old is DeviceListItem.Header && new is DeviceListItem.Header -> old.title == new.title
                    old is DeviceListItem.Device && new is DeviceListItem.Device -> old.devId == new.devId
                    else -> false
                }
            }
            override fun areContentsTheSame(old: DeviceListItem, new: DeviceListItem) = old == new
        }
    }

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is DeviceListItem.Header -> TYPE_HEADER
        is DeviceListItem.Device -> TYPE_DEVICE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> HeaderViewHolder(inflater.inflate(R.layout.item_device_header, parent, false))
            else -> DeviceViewHolder(inflater.inflate(R.layout.item_device, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is DeviceListItem.Header -> (holder as HeaderViewHolder).bind(item)
            is DeviceListItem.Device -> (holder as DeviceViewHolder).bind(item)
        }
    }

    inner class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvTitle: TextView = view.findViewById(R.id.tvSectionTitle)
        fun bind(item: DeviceListItem.Header) { tvTitle.text = item.title }
    }

    inner class DeviceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val ivIcon: ImageView = view.findViewById(R.id.ivDeviceIcon)
        private val tvName: TextView = view.findViewById(R.id.tvDeviceName)
        private val tvStatus: TextView = view.findViewById(R.id.tvDeviceStatus)
        private val tvSharedBadge: TextView = view.findViewById(R.id.tvSharedBadge)

        fun bind(item: DeviceListItem.Device) {
            tvName.text = item.name
            tvStatus.text = if (item.isOnline) itemView.context.getString(R.string.device_online) else itemView.context.getString(R.string.device_offline)
            tvStatus.setTextColor(itemView.context.getColor(if (item.isOnline) R.color.device_status_online else R.color.device_status_offline))
            tvSharedBadge.visibility = if (item.isShared) View.VISIBLE else View.GONE

            val alpha = if (item.isOnline) 1.0f else 0.4f
            ivIcon.alpha = alpha
            tvName.alpha = alpha

            if (item.iconUrl.isNotEmpty()) {
                ivIcon.load(item.iconUrl) {
                    placeholder(R.drawable.ic_device_default)
                    error(R.drawable.ic_device_default)
                }
            } else {
                ivIcon.setImageResource(R.drawable.ic_device_default)
            }

            itemView.setOnClickListener { onItemClick(item) }
            itemView.setOnLongClickListener { onItemLongClick(item); true }
        }
    }
}
