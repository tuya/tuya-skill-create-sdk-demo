package com.taojing.androidtest.ui.home.current

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.taojing.androidtest.CurrentHomeManager
import com.taojing.androidtest.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.thingclips.smart.home.sdk.bean.HomeBean

class HomeSwitchBottomSheet : BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.bottom_sheet_home_switch, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerHomeList)
        val emptyView = view.findViewById<TextView>(R.id.tvEmpty)
        val loadingView = view.findViewById<View>(R.id.progressLoading)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        loadingView.visibility = View.VISIBLE

        CurrentHomeManager.refreshHomeList { homes ->
            loadingView.visibility = View.GONE
            if (homes.isEmpty()) {
                emptyView.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                emptyView.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                recyclerView.adapter = HomeListAdapter(homes, CurrentHomeManager.currentHomeId) { selected ->
                    CurrentHomeManager.switchHome(selected.homeId, selected.name)
                    dismiss()
                }
            }
        }
    }

    companion object {
        const val TAG = "HomeSwitchBottomSheet"
    }
}

private class HomeListAdapter(
    private val homes: List<HomeBean>,
    private val currentHomeId: Long?,
    private val onItemClick: (HomeBean) -> Unit,
) : RecyclerView.Adapter<HomeListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvHomeName)
        val ivCheck: ImageView = view.findViewById(R.id.ivCheck)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_home_switch, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val home = homes[position]
        holder.tvName.text = home.name
        holder.ivCheck.visibility = if (home.homeId == currentHomeId) View.VISIBLE else View.INVISIBLE
        holder.itemView.setOnClickListener { onItemClick(home) }
    }

    override fun getItemCount(): Int = homes.size
}
