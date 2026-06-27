# Gargara Launcher

Gargara Sunucusu için özel olarak geliştirilmiş Minecraft Başlatıcısı (Launcher).

## Özellikler
- **Bağımsız Çalışma:** Oyuncuların TLauncher veya resmi launcher kullanmasına gerek kalmaz.
- **Otomatik Kurulum:** Minecraft 1.20.1, Fabric ve gerekli kütüphaneleri otomatik olarak indirir.
- **Mod Desteği:** Sunucuya özel modları doğrudan kurar ve yönetir.
- **Offline Giriş:** Premium hesaba ihtiyaç duymadan (Korsan/Çevrimdışı) kullanıcı adıyla giriş yapma imkanı.

## Nasıl Derlenir?
Bu proje Maven kullanılarak geliştirilmiştir. Derlemek için sisteminizde Java 17 veya üzeri kurulu olmalıdır.

```bash
git clone https://github.com/KullaniciAdiniz/GargaraLauncher.git
cd GargaraLauncher
mvn clean package
```

Derleme işlemi bittikten sonra `target/` klasöründeki `GargaraLauncher-1.0-SNAPSHOT-jar-with-dependencies.jar` dosyasını çalıştırabilirsiniz.

## Lisans
Bu proje MIT Lisansı altında lisanslanmıştır. Detaylar için [LICENSE](LICENSE) dosyasına bakabilirsiniz.
