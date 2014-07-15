package com.ushlanabazu.app;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.*;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;
import com.ushlanabazu.ORM.Customer;
import com.ushlanabazu.ORM.DB;
import com.ushlanabazu.utils.BitmapUtils;
import com.ushlanabazu.utils.CommonUtils;

import java.io.File;


public class MainActivity extends ActionBarActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int CM_DELETE_ID = 1;
    ListView lvData;
    DB db = DB.getDbInstance();
    private Customer customer = Customer.getCustomerInstance();
    MainSimpleCursorAdapter scAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // открываем подключение к БД
        db.open(this);

        // формируем столбцы сопоставления
        String[] from = new String[]{Customer.COLUMN_PHOTO, Customer.COLUMN_TITLE};
        int[] to = new int[]{R.id.ivImg, R.id.tvText};


        scAdapter = new MainSimpleCursorAdapter(this, R.layout.item, null, from, to, 0);
        // создааем адаптер и настраиваем список
        lvData = (ListView) findViewById(R.id.lvData);
        lvData.setAdapter(scAdapter);

        // добавляем контекстное меню к списку
        registerForContextMenu(lvData);

        // создаем лоадер для чтения данных
        getSupportLoaderManager().initLoader(0, null, this);
    }

    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, CM_DELETE_ID, 0, R.string.action_delete);
    }

    public boolean onContextItemSelected(MenuItem item) {
        if (item.getItemId() == CM_DELETE_ID) {
            // получаем из пункта контекстного меню данные по пункту списка
            AdapterContextMenuInfo acmi = (AdapterContextMenuInfo) item.getMenuInfo();
            if (acmi!=null) {
                delFile(acmi.id);
                customer.delRec(acmi.id);
                getSupportLoaderManager().getLoader(0).forceLoad();
                return true;
            }
            else
              Log.e(CommonUtils.LOG_TAG,"acmi=null in onContextItemSelected");
        }
        return super.onContextItemSelected(item);
    }

    private void delFile(long id) {
        // извлекаем id записи и удаляем соответствующую запись в БД
        String patch = customer.getPathById(id);
        if (patch!=null) {
            File f = new File(patch);
            if ((f!=null)&&(f.exists()))
                f.delete();
            else
              Log.e(CommonUtils.LOG_TAG, "Patch not found: "+patch);
        }

        else
            Log.e(CommonUtils.LOG_TAG, "Patch is null");

    }

    protected void onDestroy() {
        super.onDestroy();
        // закрываем подключение при выходе
        db.close();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bndl) {
        return new MyCursorLoader(this, db);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        scAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    static class MyCursorLoader extends CursorLoader {

        DB db;

        public MyCursorLoader(Context context, DB db) {
            super(context);
            this.db = db;
        }

        @Override
        public Cursor loadInBackground() {
            Cursor cursor =  Customer.getCustomerInstance().getAllData();
            return cursor;
        }

    }

    class MainSimpleCursorAdapter extends SimpleCursorAdapter implements SimpleCursorAdapter.ViewBinder {
        protected Context mContext;

        public MainSimpleCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
            super(context, layout, c, from, to, flags);
            mContext = context;
            setViewBinder(this);
        }

        @Override
        public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
            if ((columnIndex == Customer.INDEX_PHOTO) && (view.getId() == R.id.ivImg)) {
                try {
                    ImageView image = (ImageView) view;
                    String path = cursor.getString(columnIndex);
                    if (path != null) {
                        File imgFile = new  File(path);
                        if(imgFile.exists()){
                            BitmapUtils.loadFileIntoImageView(path, image);
                            Log.d(CommonUtils.LOG_TAG, "setViewValue "+path);
                            return true;
                        }
                    }
                    Drawable icon = mContext.getPackageManager().getApplicationIcon(CommonUtils.APP_PAKAGE);
                    if (icon!=null)
                       image.setImageDrawable(icon);
                    else
                      Log.e(CommonUtils.LOG_TAG,"icon=null in  setViewValue");

                } catch (Exception e) {
                    Log.e(CommonUtils.LOG_TAG, e.getMessage());
                }
                Log.d(CommonUtils.LOG_TAG, "setViewValue: onResume()");
                return true;
            } else {
                return false;
            }
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_search:
                getSupportLoaderManager().getLoader(0).forceLoad();
                return true;
            case R.id.action_new:
                Intent intent = new Intent(this, CreateItemActivity.class);
                intent.putExtra("mode",CommonUtils.MODE_NEW);
                startActivityForResult(intent, 0);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // запишем в лог значения requestCode и resultCode
      //  Log.d(Utils.LOG_TAG, "requestCode = " + requestCode + ", resultCode = " + resultCode);
        // если пришло ОК
        if (resultCode == RESULT_OK) {
            String name = data.getStringExtra("name");
            getSupportLoaderManager().getLoader(0).forceLoad();
            Toast.makeText(this, "Добавлено " + name, Toast.LENGTH_SHORT).show();
            // если вернулось не ОК
        }
// else {
//            Toast.makeText(this, "Ошибка заполнения", Toast.LENGTH_SHORT).show();
//        }
    }

}







