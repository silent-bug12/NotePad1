/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.notepad;

import android.annotation.SuppressLint;
import android.app.ListActivity;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Displays a list of notes with search support and formatted create time.
 */
public class NotesList extends ListActivity {

    private static final String TAG = "NotesList";
    private static final int MENU_THEME = Menu.FIRST + 10;

    private static final String[] PROJECTION = new String[] {
            NotePad.Notes._ID,
            NotePad.Notes.COLUMN_NAME_TITLE,
            NotePad.Notes.COLUMN_NAME_CREATE_DATE,
            NotePad.Notes.COLUMN_NAME_LAST_OPENED
    };

    private static final int COLUMN_INDEX_TITLE = 1;

    private SimpleCursorAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 设置自定义布局（包含搜索框）
        setContentView(R.layout.activity_note_list);

        setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);

        Intent intent = getIntent();
        if (intent.getData() == null) {
            intent.setData(NotePad.Notes.CONTENT_URI);
        }

        getListView().setOnCreateContextMenuListener(this);

        // 初始加载全部笔记
        Cursor cursor = managedQuery(
                getIntent().getData(),
                PROJECTION,
                null,
                null,
                NotePad.Notes.DEFAULT_SORT_ORDER
        );

        mAdapter = new SimpleCursorAdapter(
                this,
                R.layout.noteslist_item,
                cursor,
                new String[]{ NotePad.Notes.COLUMN_NAME_TITLE, NotePad.Notes.COLUMN_NAME_CREATE_DATE, NotePad.Notes.COLUMN_NAME_LAST_OPENED },
                new int[]{ R.id.text1, R.id.text2,R.id.text_last_opened },
                0
        );

        mAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                if (columnIndex == cursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_CREATE_DATE)) {
                    long date = cursor.getLong(columnIndex);
                    if (date <= 0) {
                        ((TextView) view).setText("—");
                    } else {
                        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                                .format(new Date(date));
                        ((TextView) view).setText(time);
                    }
                    return true;
                }// 处理最后打开时间（新增逻辑）
                else if (columnIndex == cursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_LAST_OPENED)) {
                    long lastOpenedTime = cursor.getLong(columnIndex);
                    String displayText;
                    if (lastOpenedTime == 0) {
                        displayText = "从未打开"; // 若时间为0（默认值），显示“从未打开”
                    } else {
                        displayText = "最后打开：" + formatTime(lastOpenedTime);
                    }
                    ((TextView) view).setText(displayText);
                    return true;
                }
                return false;
            }
        });

        setListAdapter(mAdapter);
        SharedPreferences prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE);
        boolean isDark = prefs.getBoolean("dark_mode", false);
        ListView listView = getListView();
        if (isDark) {
            listView.setBackgroundColor(0xFF1E1E1E); // 深灰色背景
        } else {
            listView.setBackgroundColor(0xFFFFFFFF); // 白色背景
        }

        // 绑定搜索框
        EditText searchBox = (EditText) findViewById(R.id.searchBox);
        searchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                reloadNotes(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }
    /**
     * 格式化时间戳为可读字符串（如“2023-11-18 15:30”）
     */
    private String formatTime(long timeMillis) {
        if (timeMillis == 0) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        return sdf.format(new Date(timeMillis));
    }
    private void reloadNotes(String query) {
        String selection = null;
        String[] selectionArgs = null;

        if (!TextUtils.isEmpty(query)) {
            // 搜索标题或内容（注意：PROJECTION 不含内容列，但查询时仍可筛选）
            selection = NotePad.Notes.COLUMN_NAME_TITLE + " LIKE ? OR " +
                    NotePad.Notes.COLUMN_NAME_NOTE + " LIKE ?";
            selectionArgs = new String[]{"%" + query + "%", "%" + query + "%"};
        }

        Cursor newCursor = managedQuery(
                getIntent().getData(),
                PROJECTION,
                selection,
                selectionArgs,
                NotePad.Notes.DEFAULT_SORT_ORDER
        );

        mAdapter.changeCursor(newCursor);
    }

    // ========== 以下为原有菜单和点击逻辑，保持不变 ==========

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate menu from XML resource
        getMenuInflater().inflate(R.menu.list_options_menu, menu);

        // Generate any additional actions that can be performed on the
        // overall list.  In a normal install, there are no additional
        // actions found here, but this allows other applications to extend
        // our menu with their own actions.
        Intent intent = new Intent(null, getIntent().getData());
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        menu.addIntentOptions(
                Menu.CATEGORY_ALTERNATIVE, 0, 0,
                new ComponentName(this, NotesList.class), null, intent, 0, null
        );

        return true; // 注意：不要调用 super.onCreateOptionsMenu(menu)
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        MenuItem mPasteItem = menu.findItem(R.id.menu_paste);
        mPasteItem.setEnabled(clipboard.hasPrimaryClip());

        final boolean haveItems = getListAdapter().getCount() > 0;
        if (haveItems) {
            Uri uri = ContentUris.withAppendedId(getIntent().getData(), getSelectedItemId());
            Intent[] specifics = new Intent[1];
            specifics[0] = new Intent(Intent.ACTION_EDIT, uri);
            MenuItem[] items = new MenuItem[1];
            Intent intent = new Intent(null, uri);
            intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
            menu.addIntentOptions(
                    Menu.CATEGORY_ALTERNATIVE,
                    Menu.NONE,
                    Menu.NONE,
                    null,
                    specifics,
                    intent,
                    Menu.NONE,
                    items
            );
            if (items[0] != null) {
                items[0].setShortcut('1', 'e');
            }
        } else {
            menu.removeGroup(Menu.CATEGORY_ALTERNATIVE);
        }
        return true;
    }

    @SuppressLint("UnsafeImplicitIntentLaunch")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_add) {
            startActivity(new Intent(Intent.ACTION_INSERT, getIntent().getData()));
            return true;
        } else if (item.getItemId() == R.id.menu_paste) {
            startActivity(new Intent(Intent.ACTION_PASTE, getIntent().getData()));
            return true;
        } else if (item.getItemId() == R.id.menu_theme) {
            toggleTheme();
            return true; // 必须返回 true 表示已消费该事件
        }
        return super.onOptionsItemSelected(item);
    }
    private void toggleTheme() {
        SharedPreferences prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE);
        boolean current = prefs.getBoolean("dark_mode", false);
        prefs.edit().putBoolean("dark_mode", !current).apply();
        recreate();
    }
    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info;
        try {
            info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return;
        }

        Cursor cursor = (Cursor) getListAdapter().getItem(info.position);
        if (cursor == null) return;

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_context_menu, menu);
        menu.setHeaderTitle(cursor.getString(COLUMN_INDEX_TITLE));

        Intent intent = new Intent(null, Uri.withAppendedPath(getIntent().getData(),
                Integer.toString((int) info.id)));
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                new ComponentName(this, NotesList.class), null, intent, 0, null);
    }

    @SuppressLint("UnsafeImplicitIntentLaunch")
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info;
        try {
            info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return false;
        }

        Uri noteUri = ContentUris.withAppendedId(getIntent().getData(), info.id);
        int id = item.getItemId();

        if (id == R.id.context_open) {
            startActivity(new Intent(Intent.ACTION_EDIT, noteUri));
            return true;
        } else if (id == R.id.context_copy) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newUri(getContentResolver(), "Note", noteUri));
            return true;
        } else if (id == R.id.context_delete) {
            getContentResolver().delete(noteUri, null, null);
            return true;
        }
        return super.onContextItemSelected(item);
    }

    @SuppressLint("UnsafeImplicitIntentLaunch")
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Uri uri = ContentUris.withAppendedId(getIntent().getData(), id);
        String action = getIntent().getAction();
        if (Intent.ACTION_PICK.equals(action) || Intent.ACTION_GET_CONTENT.equals(action)) {
            setResult(RESULT_OK, new Intent().setData(uri));
        } else {
            startActivity(new Intent(Intent.ACTION_EDIT, uri));
        }
    }
}