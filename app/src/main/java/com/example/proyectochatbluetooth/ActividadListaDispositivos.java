package com.example.proyectochatbluetooth;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;

public class ActividadListaDispositivos extends AppCompatActivity {
    private ListView listaDispositivosEmparejados, listaDispositivosDisponibles;
    private ArrayAdapter<String> adaptadorDispositivosEmparejados, adaptadorDispositivosDisponibles;
    private Context contexto;
    private BluetoothAdapter adaptadorBluetooth;
    private ProgressBar progresoEscanearDispositivos;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_actividad_lista_dispositivos);
        contexto = this;
        init();
    }

    private void init() {
        listaDispositivosEmparejados = findViewById(R.id.lista_dispositivos_emparejados);
        listaDispositivosDisponibles = findViewById(R.id.lista_dispositivos_disponibles);
        progresoEscanearDispositivos = findViewById(R.id.progreso_escanear_dispositivos);

        adaptadorDispositivosEmparejados = new ArrayAdapter<String>(contexto, R.layout.item_lista_dispositivo);
        adaptadorDispositivosDisponibles = new ArrayAdapter<String>(contexto, R.layout.item_lista_dispositivo);

        listaDispositivosEmparejados.setAdapter(adaptadorDispositivosEmparejados);
        listaDispositivosDisponibles.setAdapter(adaptadorDispositivosDisponibles);

        listaDispositivosDisponibles.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String info = ((TextView)view).getText().toString();
                String address = info.substring(info.length() - 17);

                Intent intent = new Intent();
                intent.putExtra("deviceAddress", address);
                setResult(RESULT_OK, intent);
                finish();
            }
        });


        adaptadorBluetooth = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> dispositivosEmparejados = adaptadorBluetooth.getBondedDevices();

        if (dispositivosEmparejados != null && dispositivosEmparejados.size() > 0){
            for (BluetoothDevice dispositivo : dispositivosEmparejados){
                adaptadorDispositivosEmparejados.add(dispositivo.getName() + "\n" + dispositivo.getAddress());
            }
        }


        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(listenerDispositivosBluetooth, intentFilter);
        IntentFilter intentFilter1 = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(listenerDispositivosBluetooth, intentFilter1);

        listaDispositivosEmparejados.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                adaptadorBluetooth.cancelDiscovery();
                String info = ((TextView) view).getText().toString();
                String address = info.substring(info.length() - 17);

                Log.d("Address", address);

                Intent intent = new Intent();
                intent.putExtra("deviceAddress", address);

                setResult(Activity.RESULT_OK, intent);
                finish();
            }
        });
    }


    private BroadcastReceiver listenerDispositivosBluetooth = new BroadcastReceiver() {
        @Override
        public void onReceive(Context contexto, Intent intent) {
            String accion = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(accion)) {
                BluetoothDevice dispositivo = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (dispositivo.getBondState() != BluetoothDevice.BOND_BONDED) {
                    adaptadorDispositivosDisponibles.add(dispositivo.getName() + "\n" + dispositivo.getAddress());
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(accion)) {

                progresoEscanearDispositivos.setVisibility(View.GONE);
                if (adaptadorDispositivosDisponibles.getCount() == 0) {
                    Toast.makeText(contexto, "No se han encontrado nuevos dispositivos", Toast.LENGTH_SHORT).show();
                } else {

                    Toast.makeText(contexto, "Selecciona el dispositivo para empezar el chat", Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_lista_dispositivos, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_buscar_dispositivos:
                buscarDispositivos();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void buscarDispositivos(){
        progresoEscanearDispositivos.setVisibility(View.VISIBLE);
        adaptadorDispositivosDisponibles.clear();

        Toast.makeText(contexto, "Escaneado iniciado", Toast.LENGTH_SHORT).show();

        if (adaptadorBluetooth.isDiscovering()) {
            adaptadorBluetooth.cancelDiscovery();
        }

        adaptadorBluetooth.startDiscovery();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (listenerDispositivosBluetooth != null) {
            unregisterReceiver(listenerDispositivosBluetooth);
        }
    }
}
