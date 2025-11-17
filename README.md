# NotePad 记事本应用 README
一、项目介绍
本项目是一款功能增强型记事本应用，在基础笔记管理能力上，集成搜索功能、时间戳管理及自定义附加功能，为用户提供更高效的笔记创建、编辑与管理体验。

二、核心功能
1. 基础笔记管理
新建笔记：支持标题与内容输入，自动记录创建时间戳。
编辑笔记：可修改已有笔记的标题和内容，实时更新修改时间戳。
笔记列表：以列表形式展示所有笔记，包含标题、创建时间等核心信息。
数据持久化：基于 SQLite 数据库存储笔记数据，确保数据安全不丢失。
2. 搜索功能
功能描述：支持在笔记列表中通过关键词快速筛选笔记，可匹配标题或内容中的关键词，帮助用户秒级定位目标笔记。
使用方法：在笔记列表页面顶部的搜索框中输入关键词，列表将实时过滤出包含该关键词的笔记。
3. 时间戳管理
创建时间戳：每条笔记自动记录创建时间（精确到毫秒），并在列表中以 “yyyy-MM-dd HH:mm:ss” 格式展示。
修改时间戳：每次编辑笔记并保存时，自动更新修改时间戳，便于用户追溯笔记的更新历史。

三、附加功能
1. 黑白背景颜色切换
功能描述：在笔记编辑页面可一键切换黑色（#1E1E1E）和白色（#FFFFFF）背景，适配夜间、强光等不同使用场景；背景色与笔记绑定，下次打开自动恢复上次选择。
使用方法：进入笔记编辑页面，点击 “切换背景” 按钮即可在黑白主题间切换。
实现亮点：自动适配文本颜色（黑色背景→白色文本，白色背景→黑色文本），保证可读性。
2. 最后一次修改时间记录与显示
功能描述：自动记录每条笔记最后一次修改的时间戳，并在笔记列表中以 “最后修改：yyyy-MM-dd HH:mm” 格式直观展示，帮助用户快速识别最新编辑的内容。
使用逻辑：在数据库表中新增last_modified列存储毫秒级时间戳，每次编辑保存时自动更新该列值为当前系统时间。

四、技术实现
1. 搜索功能核心代码（NotesList.java）
java
运行
private void setupSearch() {
    EditText searchBox = findViewById(R.id.search_box);
    searchBox.addTextChangedListener(new TextWatcher() {
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            String query = s.toString().trim();
            if (TextUtils.isEmpty(query)) {
                mAdapter.changeCursor(originalCursor);
            } else {
                // 筛选标题或内容包含关键词的笔记
                Cursor filteredCursor = getContentResolver().query(
                    NotePad.Notes.CONTENT_URI,
                    PROJECTION,
                    NotePad.Notes.COLUMN_NAME_TITLE + " LIKE ? OR " +
                    NotePad.Notes.COLUMN_NAME_NOTE + " LIKE ?",
                    new String[]{"%" + query + "%", "%" + query + "%"},
                    null
                );
                mAdapter.changeCursor(filteredCursor);
            }
        }
        // 其他接口方法...
    });
}
2. 背景颜色切换核心代码（NoteEditor.java）
java
运行
private void toggleBackground() {
    SharedPreferences prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE);
    boolean isBlack = prefs.getBoolean("black_bg", false);
    prefs.edit().putBoolean("black_bg", !isBlack).apply();
    
    LinedEditText editor = findViewById(R.id.note);
    if (isBlack) {
        editor.setBackgroundColor(Color.WHITE);
        editor.setTextColor(Color.BLACK);
    } else {
        editor.setBackgroundColor(Color.parseColor("#1E1E1E"));
        editor.setTextColor(Color.WHITE);
    }
}
3. 最后修改时间数据库扩展（NotePadProvider.java）
java
运行
// 建表语句新增最后修改时间列
db.execSQL("CREATE TABLE " + NotePad.Notes.TABLE_NAME + " ("
        + NotePad.Notes._ID + " INTEGER PRIMARY KEY,"
        + NotePad.Notes.COLUMN_NAME_TITLE + " TEXT,"
        + NotePad.Notes.COLUMN_NAME_NOTE + " TEXT,"
        + NotePad.Notes.COLUMN_NAME_CREATE_DATE + " INTEGER,"
        + NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE + " INTEGER,"
        + NotePad.Notes.COLUMN_NAME_LAST_MODIFIED + " INTEGER DEFAULT 0"
        + ");");

// 编辑时更新最后修改时间
private void updateNote(String text, String title) {
    ContentValues values = new ContentValues();
    values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, System.currentTimeMillis());
    values.put(NotePad.Notes.COLUMN_NAME_LAST_MODIFIED, System.currentTimeMillis());
    // 其他保存逻辑...
}

五、兼容性
支持 Android 5.0（API 21）及以上版本。
数据库升级时通过 ALTER TABLE 安全添加新列，确保原有笔记数据不丢失。
