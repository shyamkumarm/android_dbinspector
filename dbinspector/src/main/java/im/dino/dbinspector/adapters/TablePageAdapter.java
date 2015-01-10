package im.dino.dbinspector.adapters;

import android.annotation.TargetApi;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.preference.PreferenceManager;
import android.widget.TableRow;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import im.dino.dbinspector.helpers.CursorOperation;
import im.dino.dbinspector.helpers.DatabaseHelper;
import im.dino.dbinspector.helpers.DisplayHelper;
import im.dino.dbview.R;

/**
 * Created by dino on 27/02/14.
 */
public class TablePageAdapter {

    private final Context mContext;

    private final File mDatabaseFile;

    private final String mTableName;

    private int mRowsPerPage = 10;

    private int mPosition = 0;

    private int mCount = 0;

    private int mPaddingPx;

    public TablePageAdapter(Context context, File databaseFile, String tableName, int startPage) {

        mContext = context;
        mDatabaseFile = databaseFile;
        mTableName = tableName;
        mPaddingPx = DisplayHelper.dpToPx(mContext, 5);

        String keyRowsPerPage = mContext.getString(R.string.dbinspector_pref_key_rows_per_page);
        String defaultRowsPerPage = mContext.getString(R.string.dbinspector_rows_per_page_default);
        String rowsPerPage = PreferenceManager.getDefaultSharedPreferences(mContext)
                .getString(keyRowsPerPage, defaultRowsPerPage);
        mRowsPerPage = Integer.valueOf(rowsPerPage);
        mPosition = mRowsPerPage * startPage;
    }

    public List<TableRow> getStructure() {

        CursorOperation<List<TableRow>> operation = new CursorOperation<List<TableRow>>(mDatabaseFile) {
            @Override
            public Cursor provideCursor(SQLiteDatabase database) {
                return database.rawQuery(String.format(DatabaseHelper.PRAGMA_FORMAT, mTableName), null);
            }

            @Override
            public List<TableRow> provideResult(SQLiteDatabase database, Cursor cursor) {
                cursor.moveToFirst();
                return getTableRows(cursor, true);
            }
        };

        return operation.execute();
    }

    public List<TableRow> getContentPage() {

        CursorOperation<List<TableRow>> operation = new CursorOperation<List<TableRow>>(mDatabaseFile) {
            @Override
            public Cursor provideCursor(SQLiteDatabase database) {
                return database.query(mTableName, null, null, null, null, null, null);
            }

            @Override
            public List<TableRow> provideResult(SQLiteDatabase database, Cursor cursor) {
                mCount = cursor.getCount();
                cursor.moveToPosition(mPosition);
                return getTableRows(cursor, false);
            }
        };

        return operation.execute();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private List<TableRow> getTableRows(Cursor cursor, boolean allRows) {

        List<TableRow> rows = new ArrayList<>();

        TableRow header = new TableRow(mContext);

        for (int col = 0; col < cursor.getColumnCount(); col++) {
            TextView textView = new TextView(mContext);
            textView.setText(cursor.getColumnName(col));
            textView.setPadding(mPaddingPx, mPaddingPx / 2, mPaddingPx, mPaddingPx / 2);
            textView.setTypeface(Typeface.DEFAULT_BOLD);
            header.addView(textView);
        }

        rows.add(header);

        boolean alternate = true;

        if (cursor.getCount() == 0) {
            return rows;
        }

        do {
            TableRow row = new TableRow(mContext);

            for (int col = 0; col < cursor.getColumnCount(); col++) {
                TextView textView = new TextView(mContext);
                if (cursor.getType(col) == Cursor.FIELD_TYPE_BLOB) {
                    textView.setText("(data)");
                } else {
                    textView.setText(cursor.getString(col));
                }
                textView.setPadding(mPaddingPx, mPaddingPx / 2, mPaddingPx, mPaddingPx / 2);

                if (alternate) {
                    textView.setBackgroundColor(Color.rgb(242, 242, 242));
                }

                row.addView(textView);
            }

            alternate = !alternate;
            rows.add(row);

        } while (cursor.moveToNext() && (allRows || rows.size() <= mRowsPerPage));

        return rows;
    }

    public void nextPage() {
        if (mPosition + mRowsPerPage < mCount) {
            mPosition += mRowsPerPage;
        }
    }

    public void previousPage() {
        if (mPosition - mRowsPerPage >= 0) {
            mPosition -= mRowsPerPage;
        }
    }

    public boolean hasNext() {
        return mPosition + mRowsPerPage < mCount;
    }

    public boolean hasPrevious() {
        return mPosition - mRowsPerPage >= 0;
    }

    public int getPageCount() {
        return (int) Math.ceil((float) mCount / mRowsPerPage);
    }

    public int getCurrentPage() {
        return (mPosition / mRowsPerPage) + 1;
    }

}
