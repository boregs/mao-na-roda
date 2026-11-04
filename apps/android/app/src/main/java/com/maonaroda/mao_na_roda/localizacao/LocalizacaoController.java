package com.maonaroda.mao_na_roda.localizacao;

import android.Manifest;
import android.content.*;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.location.Location;
import androidx.core.location.LocationListenerCompat;
import androidx.core.location.LocationRequestCompat;
import androidx.core.content.ContextCompat;
import androidx.core.location.LocationManagerCompat;
import androidx.core.os.CancellationSignal;
import com.maonaroda.mao_na_roda.data.DadosRotas.Ponto;
import java.util.function.Consumer;

public final class LocalizacaoController {
    private CancellationSignal sinal;
    private LocationManager atualizacoes;
    private LocationListenerCompat ouvinte;

    public boolean acompanhar(Context context, Consumer<Location> receber, Runnable indisponivel) {
        cancelar();
        var manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        try {
            if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) return false;
            ouvinte = new LocationListenerCompat() {
                @Override public void onLocationChanged(Location location) {
                    if (ouvinte == this) receber.accept(location);
                }
                @Override public void onProviderDisabled(String provider) {
                    if (ouvinte == this) indisponivel.run();
                }
            };
            atualizacoes = manager;
            LocationManagerCompat.requestLocationUpdates(manager, LocationManager.GPS_PROVIDER,
                    new LocationRequestCompat.Builder(2000).build(), ContextCompat.getMainExecutor(context), ouvinte);
            return true;
        } catch (SecurityException | IllegalArgumentException e) {
            cancelar();
            return false;
        }
    }
    public void obter(Context context, Consumer<Ponto> sucesso, Runnable falha) {
        cancelar();
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) { falha.run(); return; }
        var manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        String provider = manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ? LocationManager.NETWORK_PROVIDER : LocationManager.GPS_PROVIDER;
        sinal = new CancellationSignal();
        try {
            LocationManagerCompat.getCurrentLocation(manager, provider, sinal, ContextCompat.getMainExecutor(context), location -> {
                if (location == null) falha.run(); else sucesso.accept(new Ponto(location.getLongitude(), location.getLatitude()));
            });
        } catch (SecurityException | IllegalArgumentException e) { falha.run(); }
    }
    public void cancelar() {
        if (sinal != null) { sinal.cancel(); sinal = null; }
        if (ouvinte != null) {
            var anterior = ouvinte;
            ouvinte = null;
            LocationManagerCompat.removeUpdates(atualizacoes, anterior);
            atualizacoes = null;
        }
    }
}
