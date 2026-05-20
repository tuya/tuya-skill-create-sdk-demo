package com.taojing.androidtest.ui.home

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

class SwipeDeleteCallback(
    private val adapter: DeviceListAdapter,
    private val onSwiped: (position: Int, device: DeviceListItem.Device) -> Unit,
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

    private val bgPaint = Paint().apply {
        color = Color.parseColor("#FF4444")
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 40f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        if (viewHolder !is DeviceListAdapter.DeviceViewHolder) return makeMovementFlags(0, 0)
        return super.getMovementFlags(recyclerView, viewHolder)
    }

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean = false

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val pos = viewHolder.bindingAdapterPosition
        if (pos == RecyclerView.NO_POSITION) return
        val item = adapter.currentList.getOrNull(pos)
        if (item is DeviceListItem.Device) onSwiped(pos, item)
    }

    override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
        if (viewHolder !is DeviceListAdapter.DeviceViewHolder) {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            return
        }

        val itemView = viewHolder.itemView

        if (dX < 0) {
            val bgRect = RectF(itemView.right + dX, itemView.top.toFloat(), itemView.right.toFloat(), itemView.bottom.toFloat())
            c.drawRect(bgRect, bgPaint)

            val pos = viewHolder.bindingAdapterPosition
            val item = if (pos != RecyclerView.NO_POSITION) adapter.currentList.getOrNull(pos) else null
            val label = if (item is DeviceListItem.Device && item.isShared) "取消共享" else "删除"

            val centerX = itemView.right + dX / 2
            val centerY = itemView.top + itemView.height / 2f
            val textOffset = (textPaint.descent() + textPaint.ascent()) / 2
            c.drawText(label, centerX, centerY - textOffset, textPaint)
        }

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }
}
