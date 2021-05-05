package com.example.proyectochatbluetooth;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.nio.charset.CharacterCodingException;


@android.support.annotation.RequiresApi(api = Build.VERSION_CODES.CUPCAKE)
public class MainActivity extends AppCompatActivity {
    private BluetoothAdapter adaptadorBluetooth;
    private Context contexto;
    private ChatUtils chatUtils;

    private ListView listMainChat;
    private EditText edCreateMessage;
    private Button btnSendMessage;
    private ArrayAdapter<String> adapterMainChat;

    private final int UBICACION_SOLICITUD_PERMISO = 101;
    private final int SELECCIONAR_DISPOSITIVO = 102;

    public static final int MESSAGE_STATE_CHANGED = 0;
    public static final int MESSAGE_READ = 1;
    public static final int MESSAGE_WRITE = 2;
    public static final int MESSAGE_DEVICE_NAME = 3;
    public static final int MESSAGE_TOAST = 4;

    public static final String DEVICE_NAME = "deviceName";
    public static final String TOAST = "toast";
    private String dispositivoConectado;

    private Handler handler =  new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what){
                case MESSAGE_STATE_CHANGED:
                    switch (msg.arg1){
                        case ChatUtils.STATE_NONE:
                            setState("Desconectado");
                            break;
                        case ChatUtils.STATE_LISTEN:
                            setState("Desconectado");
                            break;
                        case ChatUtils.STATE_CONNECTING:
                            setState("Conectando...");
                            break;
                        case ChatUtils.STATE_CONNECTED:
                            setState("Conectado: " + dispositivoConectado);
                            break;
                    }
                    break;
                case MESSAGE_READ:
                    byte[] buffer = (byte[]) msg.obj;
                    String inputBuffer = new String(buffer, 0, msg.arg1);
                    adapterMainChat.add(dispositivoConectado + ": " + inputBuffer);

                    break;
                case MESSAGE_WRITE:
                    byte[] buffer1 = (byte[]) msg.obj;
                    String outputBuffer = new String(buffer1);
                    adapterMainChat.add("Yo: " + outputBuffer);
                    break;
                case MESSAGE_DEVICE_NAME:
                    dispositivoConectado = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(contexto, dispositivoConectado, Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(contexto, msg.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
                    break;
            }

            return false;
        }
    });

    private void setState(CharSequence subTitulo){
        getSupportActionBar().setSubtitle(subTitulo);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        contexto = this;

        init();
        initBluetooth();
        chatUtils = new ChatUtils(contexto, handler);
    }

    private void init(){
        listMainChat = findViewById(R.id.lista_conversacion);
        edCreateMessage = findViewById(R.id.ed_ingresar_mensaje);
        btnSendMessage = findViewById(R.id.btn_enviar);

        adapterMainChat = new ArrayAdapter<String>(contexto, R.layout.message_layout);
        listMainChat.setAdapter(adapterMainChat);

        btnSendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String message = edCreateMessage.getText().toString();
                if (!message.isEmpty()){
                    edCreateMessage.setText("");
                    chatUtils.write(message.getBytes());
                }

            }
        });
    }

    private void initBluetooth() {
        adaptadorBluetooth = BluetoothAdapter.getDefaultAdapter();
        if (adaptadorBluetooth == null) {
            Toast.makeText(contexto, "Bluetooth no fue encontrado en este dispositivo", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_buscar_dispositivos:
                revisar_permisos();
                return true;
            case R.id.menu_encender_bluetooth:
                activarBluetooth();
                return true;
            case R.id.menu_info:
                    Toast.makeText(contexto, "Autores: Juan Camilo García Braham y Juan Esteban Corrales Gallego", Toast.LENGTH_SHORT).show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void revisar_permisos() {
        if (ContextCompat.checkSelfPermission(contexto, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, UBICACION_SOLICITUD_PERMISO);
        }
        else {
            Intent intent = new Intent(contexto, ActividadListaDispositivos.class);
            startActivityForResult(intent, SELECCIONAR_DISPOSITIVO);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SELECCIONAR_DISPOSITIVO && resultCode == RESULT_OK){
            String address = data.getStringExtra("deviceAddress");
            Toast.makeText(contexto, "Address" + address, Toast.LENGTH_SHORT).show();
            chatUtils.connect(adaptadorBluetooth.getRemoteDevice(address));
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == UBICACION_SOLICITUD_PERMISO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Intent intent = new Intent(contexto, ActividadListaDispositivos.class);
                startActivityForResult(intent, SELECCIONAR_DISPOSITIVO);


            }
            else {
                new AlertDialog.Builder(contexto)
                        .setCancelable(false)
                        .setMessage("Esta app necesita acceder a ciertas funcionalidades del celular para funcionar correctamente.")
                        .setPositiveButton("Conceder acceso", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                revisar_permisos();
                            }
                        })
                        .setNegativeButton("Rechazar", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                MainActivity.this.finish();
                            }
                        }).show();
            }
        }
        else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }

    }

    private void activarBluetooth() {
        if (adaptadorBluetooth.isEnabled()) {
            adaptadorBluetooth.enable();
        }
        else {
            adaptadorBluetooth.enable();
            Toast.makeText(contexto, "Bluetooth ha sido activado", Toast.LENGTH_SHORT).show();
        }

        if (adaptadorBluetooth.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE){
            Intent discoveryIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoveryIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoveryIntent);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chatUtils != null){
            chatUtils.stop();
        }
    }
}


