package com.chi.expandablerecyclerviewadapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

/**
 * Created by Administrator on 2019/12/24.
 */

public abstract class ExpandableRecyclerViewAdapter<K, V> extends RecyclerView
        .Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "ExpandableRecyclerViewA";
    private Context mContext; // 上下文对象
    private int mGroupLayoutRes; // 一级条目布局
    private int mChildLayoutRes; // 二级条目布局
    private List<GroupData<K, V>> mData; // 数据集合
    private int mSize;

    public ExpandableRecyclerViewAdapter(Context mContext, int mGroupLayoutRes, int
            mChildLayoutRes, List<GroupData<K, V>> mData) {
        this.mContext = mContext;
        this.mGroupLayoutRes = mGroupLayoutRes;
        this.mChildLayoutRes = mChildLayoutRes;
        this.mData = mData;
        Log.d(TAG, "ExpandableRecyclerViewAdapter: " + mData.size());
    }

    /**
     * 点击与长按监听接口
     */
    public interface OnItemClickListener {

        void onItemLongClick(View view, int position, Object object);

        void onItemClick(View view, int position, Object object);
    }


    private OnItemClickListener itemClickListener;

    public void setOnItemClickListener(OnItemClickListener itemClickListener) {
        this.itemClickListener = itemClickListener;
    }

    /**
     * @param position
     * @return 数据类型，对应@onCreateViewHolder的viewType参数 默认返回值为0
     */
    @Override
    public int getItemViewType(int position) {
        //通过位置判断type，因为数据传入后顺序不变，可通过数据来判断当前位置是哪一类数据
        int currentPosition = -1;
        for (GroupData data : mData) {
            if (!data.isExpand) { // 未展开状态
                currentPosition = currentPosition + 1; // 此处加1，是加未展开条目的一级条目行
                // 如果是未展开状态，之前计算出来的currentPosition加上1,正好是当前未展开条目的一级条目的位置
                if (currentPosition == position) {
                    return ExpandableViewHolder.GROUP;
                }
            } else { // 展开状态
                //算上group
                currentPosition = currentPosition + 1; // 此处加1，是加展开条目的一级条目行
                // 如果是展开状态，之前计算出来的currentPosition加上1,正好是当前展开条目的一级条目的位置
                if (currentPosition == position) {
                    return ExpandableViewHolder.GROUP;
                }
                //算上children，通过比较大小确定是否是当前GroupData中的child
                currentPosition = currentPosition + data.getSubItems().size();
                // position大于某一组展开条目的一级条目，小于二级条目最大数，那肯定是某一个二级条目
                if (position <= currentPosition) {
                    return ExpandableViewHolder.CHILD;
                }
            }
        }
        return ExpandableViewHolder.GROUP;
    }

    @Override
    public final RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v;
        RecyclerView.ViewHolder viewHolder = null;
        Log.d(TAG, "onCreateViewHolder: " + viewType);
        if (viewType == ExpandableViewHolder.GROUP) {
            v = LayoutInflater.from(parent.getContext()).inflate(mGroupLayoutRes, parent, false);
            viewHolder = new GroupViewHolder(v);
        } else if (viewType == ExpandableViewHolder.CHILD) {
            v = LayoutInflater.from(parent.getContext()).inflate(mChildLayoutRes, parent, false);
            viewHolder = new ChildViewHolder(v);
        }
        return viewHolder;
    }

    @Override
    public final void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (holder instanceof GroupViewHolder) {
                    GroupData<K, V> data = getData(holder.getAdapterPosition());
                    data.setExpand(!data.isExpand); // 折叠状态切换
                    mSize = 0; // 折叠状态变化，需要重新计算ItemCount
                    // notifyDataSetChanged();//最准确，但数据多时性能有影响
                    // notifyItemRangeChanged(viewHolder.getAdapterPosition()+1,getItemCount());
                    // 需要考虑到holder的旧索引问题，暂无太好的办法去规避
                    if (!data.isExpand) {
                        notifyItemRangeRemoved(holder.getAdapterPosition() + 1, data.getSubItems
                                ().size());
                    } else {
                        notifyItemRangeInserted(holder.getAdapterPosition() + 1, data.getSubItems
                                ().size());
                    }
                    // 没有subItem的group没有展开需求，可用于一级目录状态的切换，比如展开/收缩图标的切换
                    if (data.getSubItems().size() > 0) {
                        //onClickParentItem((ExpandableViewHolder) holder, position, data.isExpand);
                        boolean isExpand = getIsExpand(position);
                        boolean haveSubItem = getData(position).getSubItems().size() > 0;
                        onExpandableBindView((ExpandableViewHolder) holder, position,
                                haveSubItem, isExpand);
                    }

                    if (itemClickListener != null) {
                        itemClickListener.onItemClick(holder.itemView, holder.getLayoutPosition(),
                                getItem(holder.getAdapterPosition()));
                    }
                }
            }
        });
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (itemClickListener != null) {
                    itemClickListener.onItemLongClick(holder.itemView, holder.getLayoutPosition()
                            , getItem(holder.getAdapterPosition()));
                }
                return true;
            }
        });

        boolean isExpand = false;
        boolean haveSubItem = false;
        // 只有当position是一级目录时才判断是否处于展开状态以及是否有子目录
        if (holder instanceof GroupViewHolder) {
            isExpand = getIsExpand(position);
            haveSubItem = getData(position).getSubItems().size() > 0;
        }
        onExpandableBindView((ExpandableViewHolder) holder, position, haveSubItem, isExpand);
    }

    public abstract void onExpandableBindView(ExpandableViewHolder holder, int position, boolean
            haveSubItem, boolean isExpand);

    @Override
    public int getItemCount() {
        Log.d(TAG, "getItemCount: ");
        if (mSize == 0) {
            int totalSize = 0;
            for (GroupData data : mData) {
                // 如果是展开状态，count则为二级元素总数加上一个一级元素
                totalSize += (data.isExpand ? data.getSubItems().size() + 1 : 1);
            }
            mSize = totalSize;
        }
        return mSize;
    }

    /**
     * 根据索引返回Unit中的K或V
     *
     * @param position 索引
     * @return K/V 算法和@getItemViewType方法中算法相同
     */
    public Object getItem(int position) {
        int currentPosition = -1;
        for (GroupData data : mData) {
            if (!data.isExpand) {
                currentPosition = currentPosition + 1;
                if (currentPosition == position) {
                    return data.getGroupItem();
                }
            } else {
                //算上group
                currentPosition = currentPosition + 1;
                if (currentPosition == position) {
                    return data.getGroupItem();
                }
                //算上children，通过计算确定是当前GroupData的child的索引
                currentPosition = currentPosition + data.getSubItems().size();
                if (position <= currentPosition) {
                    int unitChildIndex = data.getSubItems().size() - 1 - (currentPosition -
                            position);
                    return data.getSubItems().get(unitChildIndex);
                }
            }
        }
        return null;
    }

    /**
     * 根据索引确定返回某个数据集
     *
     * @param position 索引
     * @return GroupData
     */
    public GroupData<K, V> getData(int position) {
        int currentPosition = -1;
        for (GroupData<K, V> data : mData) {
            //算上group
            currentPosition += data.isExpand ? data.getSubItems().size() + 1 : 1;
            if (position <= currentPosition)
                return data;
        }
        return null;
    }

    /**
     * @param position 索引
     * @return 返回索引项对应的组是否是展开状态
     */
    public boolean getIsExpand(int position) {
        int currentPosition = -1;
        for (GroupData<K, V> data : mData) {
            //算上group
            currentPosition += data.isExpand ? data.getSubItems().size() + 1 : 1;
            if (position <= currentPosition)
                return data.isExpand;
        }
        return false;
    }

    /**
     * 一组(一级、二级数据)
     * 一个GroupItem下面有一个或多个SubItem
     * 使用泛型类型，让user自定义GroupItem和SubItem结构
     */

    public static class GroupData<K, V> {

        public boolean isExpand = false;
        private K groupItem;
        private List<V> subItems;

        /**
         * @param groupItem 一级条目
         * @param subItems  二级条目
         */
        public GroupData(K groupItem, List<V> subItems) {
            this.groupItem = groupItem;
            this.subItems = subItems;
        }

        /**
         * @param groupItem 一级条目
         * @param subItems  二级条目
         * @param isExpand  是否展开
         */
        public GroupData(K groupItem, List<V> subItems, boolean isExpand) {
            this.groupItem = groupItem;
            this.subItems = subItems;
            this.isExpand = isExpand;
        }

        public K getGroupItem() {
            return groupItem;
        }

        public List<V> getSubItems() {
            return subItems;
        }

        public void setExpand(boolean expand) {
            this.isExpand = expand;
        }
    }

    /*------------------ 关于数据的增删改  ------------------*/
    public void add(GroupData<K, V> element) {
        mData.add(element);
        mSize = 0;
        notifyDataSetChanged();
    }

    public void add(List<GroupData<K, V>> elemList) {
        mData.addAll(elemList);
        mSize = 0;
        notifyDataSetChanged();
    }

    public void remove(GroupData<K, V> elem) {
        mData.remove(elem);
        mSize = 0;
        notifyDataSetChanged();
    }

    public void replace(List<GroupData<K, V>> elemList) {
        mData.clear();
        mData.addAll(elemList);
        mSize = 0;
        notifyDataSetChanged();
    }

    /*------------------ 一些准备工作，定义数据或Holder之类  ------------------*/

    protected static abstract class ExpandableViewHolder extends RecyclerView.ViewHolder {

        static final int GROUP = 0;
        static final int CHILD = 1;

        private SparseArray<View> views = new SparseArray<>();
        private View convertView;

        public ExpandableViewHolder(@NonNull View itemView) {
            super(itemView);
            this.convertView = itemView;
        }

        @SuppressWarnings("unchecked")
        public <T extends View> T getView(int resId) {
            View v = views.get(resId);
            if (null == v) {
                v = convertView.findViewById(resId);
                views.put(resId, v);
            }
            return (T) v;
        }
    }

    protected static class GroupViewHolder extends ExpandableViewHolder {

        public GroupViewHolder(@NonNull View itemView) {
            super(itemView);
        }

    }

    protected static class ChildViewHolder extends ExpandableViewHolder {

        public ChildViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
