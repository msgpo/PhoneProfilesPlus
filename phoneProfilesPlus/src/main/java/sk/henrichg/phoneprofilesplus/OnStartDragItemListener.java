package sk.henrichg.phoneprofilesplus;

import android.support.v7.widget.RecyclerView;

interface OnStartDragItemListener {

    /**
     * Called when a view is requesting a start of a drag.
     *
     * @param viewHolder The holder of the view to drag.
     */
    void onStartDrag(RecyclerView.ViewHolder viewHolder);

}
