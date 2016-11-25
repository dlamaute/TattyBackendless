package com.wearabletattoos.diana.tatty;

        import android.app.ListActivity;
        import android.app.ListFragment;
        import android.support.v7.app.ActionBarActivity;
        import android.os.Bundle;
        import android.view.Menu;
        import android.view.MenuItem;
        import android.widget.ArrayAdapter;

        //import com.parse.ParseUser;

        import com.backendless.Backendless;
        import com.backendless.BackendlessUser;

        import org.json.JSONArray;
        import org.json.JSONException;
        import org.json.JSONObject;

        import java.util.ArrayList;


public class MyTattoosActivity extends ListActivity {

    //private ParseUser user;
    private BackendlessUser user;
    private ArrayList<String> tattooList;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_tattoos);

        //user = ParseUser.getCurrentUser();
        user = Backendless.UserService.CurrentUser();

        //tattooList = (ArrayList<String>) user.getProperty("tattoos");

        /*if (tattooListJSON != null) {
            for (int i=0;i<tattooListJSON.length();i++){
                try {
                    tattooList.add(tattooListJSON.get(i).toString());
                }
                catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, tattooList);
            setListAdapter(adapter);
        }*/
        //else you should probably say "user has no tattoos"
    }
    //should be listview
    //contains list of imgbuttons describing tattoos which belong to current user
}