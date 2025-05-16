package com.findsong.findsongapi.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Configuración para el sistema de caché de la aplicación.
 * Optimiza las respuestas al almacenar resultados frecuentes en memoria.
 * Utiliza Caffeine como proveedor de caché de alto rendimiento.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Crea un gestor de caché con Caffeine para la aplicación.
     * Configura un tiempo de expiración para evitar el consumo excesivo de memoria.
     * 
     * @return CacheManager configurado
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCacheNames(Arrays.asList(
                "spotifyArtists",
                "spotifyAlbums",
                "shazamResults"));

        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .recordStats());

        return cacheManager;
    }
}