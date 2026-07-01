
## GitHub Sürüm Yönetimi (Release)
- Github'a yeni bir sürüm yüklemeden (release create / upload) önce, her zaman kurulum dosyasının (.exe) ve kaynak .jar dosyasının oluşturulma / değiştirilme saatlerini (timestamp) kontrol et.
- Derleme yaparken SADECE mvn compile veya mvn assembly:single kullanma. Hataları gizleyebilir. Daima mvn clean package komutuyla temiz bir derleme yapıldığından emin ol.
- Eğer Inno Setup (ISCC.exe) çalıştırılıyorsa, öncesinde hedef JAR dosyasının saniyeler önce başarıyla oluşturulup oluşturulmadığını doğrula. Asla eski önbelleğe alınmış dosyaları yayınlama.
