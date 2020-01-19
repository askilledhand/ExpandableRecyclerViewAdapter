# ExpandableRecyclerViewAdapter
可展开RecyclerView适配器

例：

     private void init() {

        recyclerView = findViewById(R.id.recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        List<GroupData<Directory, SubItem>> data = getData();
        DirsAdapter adapter = new DirsAdapter(this, R.layout.item_group, R.layout.item_child, data);

        adapter.setOnItemClickListener(new ExpandableRecyclerViewAdapter.OnItemClickListener() {

            @Override
            public void onItemLongClick(View view, int position, Object object) {
                Log.d(TAG, "onItemLongClick: " + position);
            }

            @Override
            public void onItemClick(View view, int position, Object object) {
                if (object instanceof Directory) {
                    Log.d(TAG, "onItemClick: Directory");
                } else if (object instanceof SubItem) {
                    Log.d(TAG, "onItemClick: SubItem");
                }
            }
        });
        recyclerView.setAdapter(adapter);
    }

    private List<GroupData<Directory, SubItem>> getData() {
        List<GroupData<Directory, SubItem>> dirs = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Directory directory = new Directory("level_1_" + i, i * 10);
            List<SubItem> subItems = new ArrayList<>();
            for (int j = 0; j < i; j++) {
                SubItem subItem = new SubItem("level_2_" + j, j * 100);
                subItems.add(subItem);
            }
            ExpandableRecyclerViewAdapter.GroupData<Directory, SubItem> unit = new GroupData<>(directory, subItems);
            dirs.add(unit);
        }
        return dirs;
    }
    
    
    public class DirsAdapter extends ExpandableRecyclerViewAdapter<Directory, SubItem> {

            public DirsAdapter(Context mContext, int mGroupLayoutRes, int mChildLayoutRes,
                               List<GroupData<Directory, SubItem>> mData) {
                super(mContext, mGroupLayoutRes, mChildLayoutRes, mData);
            }

            @Override
            public void onExpandableBindView(ExpandableRecyclerViewAdapter.ExpandableViewHolder holder,
                                             int position, boolean haveSubItem, boolean isExpand) {
                if (holder instanceof GroupViewHolder) {
                    TextView tvName = holder.getView(R.id.tv_name);
                    TextView tvSize = holder.getView(R.id.tv_num);
                    ImageView image = holder.getView(R.id.image);

                    Directory directory = (Directory) getItem(position);
                    tvName.setText(directory.name);
                    tvSize.setText(directory.size + "");
                    if (haveSubItem) {
                        if (isExpand) {
                            image.setImageResource(R.mipmap.ic_launcher);
                        } else {
                            image.setImageResource(R.drawable.ic_launcher_foreground);
                        }
                    } else {
                        image.setImageResource(0);
                    }
                }

                if (holder instanceof ChildViewHolder) {
                    TextView tvName = holder.getView(R.id.tv_child_name);
                    TextView tvSize = holder.getView(R.id.tv_child_num);

                    SubItem subItem = (SubItem) getItem(position);
                    tvName.setText(subItem.name);
                    tvSize.setText(subItem.size + "");
                }
            }
        }
