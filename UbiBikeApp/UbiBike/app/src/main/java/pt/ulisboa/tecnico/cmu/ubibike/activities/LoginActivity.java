package pt.ulisboa.tecnico.cmu.ubibike.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import pt.ulisboa.tecnico.cmu.ubibike.R;
import pt.ulisboa.tecnico.cmu.ubibike.UbiApp;
import pt.ulisboa.tecnico.cmu.ubibike.data.StationsData;
import pt.ulisboa.tecnico.cmu.ubibike.data.UserLoginData;
import pt.ulisboa.tecnico.cmu.ubibike.data.WifiDirectData;
import pt.ulisboa.tecnico.cmu.ubibike.domain.Station;
import pt.ulisboa.tecnico.cmu.ubibike.domain.User;
import pt.ulisboa.tecnico.cmu.ubibike.remote.rest.StationServiceREST;
import pt.ulisboa.tecnico.cmu.ubibike.remote.rest.UserServiceREST;
import pt.ulisboa.tecnico.cmu.ubibike.remote.rest.UtilREST;
import pt.ulisboa.tecnico.cmu.ubibike.services.UserSynchronizeService;
import pt.ulisboa.tecnico.cmu.ubibike.services.WifiDirectService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends Activity {

    protected EditText usernameEditText;
    protected EditText passwordEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //APP Entry Point (Activity)!!!!
        super.onCreate(savedInstanceState);
        startService(new Intent(getBaseContext(), UserSynchronizeService.class));

        if(UserLoginData.getUserLoggedInStatus(this)){
            User user = UserLoginData.getUser(this);
            UbiApp.getInstance().setUser(user);

            if (WifiDirectService.isRunning()) {
                stopService(new Intent(getBaseContext(), WifiDirectService.class));
            }

            startWifiDirect(user);

            finish();
            startActivity(new Intent(getBaseContext(), HomeActivity.class));
            return;
        }
        setContentView(R.layout.activity_login);


        TextView btn=(TextView) findViewById(R.id.create_account_button);
        btn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                createAccount(v);
            }
        });

        usernameEditText = (EditText)findViewById(R.id.login_username);
        passwordEditText = (EditText)findViewById(R.id.login_password);

    }

    private void startWifiDirect(final User user) {

        new Thread() {
            public void run() {
                Intent serviceIntent = new Intent(getBaseContext(), WifiDirectService.class);
                serviceIntent.putExtra("USERNAME", user.getUsername());
                startService(serviceIntent);
            }
        }.start();

        WifiDirectData.setIsEnabled(getApplicationContext(), true);
    }



    public void createAccount(View view) {
        Intent intent = new Intent(getBaseContext(), CreateAccountActivity.class);
        startActivity(intent);
    }

    public void submitLogin(View view) {
        boolean valid = true;
        String username = usernameEditText.getText().toString();
        String password = passwordEditText.getText().toString();

        if(username.isEmpty()){
            usernameEditText.setError(getString(R.string.field_required_hint));
            valid = false;
        }
        else if(password.isEmpty()){
            passwordEditText.setError(getString(R.string.field_required_hint));
            valid = false;
        }
        if(!valid)
            return;

        UserServiceREST userService = UtilREST.getRetrofit().create(UserServiceREST.class);
        Call<User> call = userService.login(username, password);
        call.enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.code() == UtilREST.HTTP_OK) {
                    User user = response.body();
                    getAllStations();
                    UbiApp.getInstance().setUser(user);
                    UserLoginData.setUserLoggedIn(getBaseContext(), user.getUsername(), user);

                    if (WifiDirectService.isRunning()) {
                        stopService(new Intent(getBaseContext(), WifiDirectService.class));
                    }
                    startWifiDirect(user);
                    finish();
                    startActivity(new Intent(getBaseContext(), HomeActivity.class));
                } else {
                    Toast.makeText(getBaseContext(), R.string.login_failed_toast, Toast.LENGTH_LONG).show();
                }
            }
            @Override
            public void onFailure(Call<User> call, Throwable t) {
                Toast.makeText(getBaseContext(), R.string.impossible_connect_server_toast, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void getAllStations(){
        if(!StationsData.getStationsSaved(getBaseContext())){
            StationServiceREST stationService = UtilREST.getRetrofit().create(StationServiceREST.class);
            Call<List<Station>> call = stationService.getStations(StationServiceREST.STATION_DETAIL_LOW);
            call.enqueue(new Callback<List<Station>>() {
                @Override
                public void onResponse(Call<List<Station>> call, Response<List<Station>> response) {
                    if (response.code() == UtilREST.HTTP_OK) {
                        List<Station> stations = response.body();
                        StationsData.setStations(getBaseContext(), stations);
                    }
                }
                @Override
                public void onFailure(Call<List<Station>> call, Throwable t) {
                    Toast.makeText(getBaseContext(), R.string.impossible_connect_server_toast, Toast.LENGTH_LONG).show();
                }
            });
        }


    }



}
