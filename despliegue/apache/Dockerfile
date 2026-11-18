# BioLearn · Apache 2.4 para publicar el frontend y reenviar la API al backend
FROM httpd:2.4
# Activa los módulos de proxy inverso, cabeceras, compresión, reescritura y HTTPS (vienen comentados en la imagen oficial)
RUN sed -i \
      -e 's/^#\(LoadModule proxy_module \)/\1/' \
      -e 's/^#\(LoadModule proxy_http_module \)/\1/' \
      -e 's/^#\(LoadModule headers_module \)/\1/' \
      -e 's/^#\(LoadModule deflate_module \)/\1/' \
      -e 's/^#\(LoadModule rewrite_module \)/\1/' \
      -e 's/^#\(LoadModule ssl_module \)/\1/' \
      -e 's/^#\(LoadModule socache_shmcb_module \)/\1/' \
      conf/httpd.conf \
 && echo 'Include conf/extra/biolearn.conf' >> conf/httpd.conf
COPY biolearn.conf conf/extra/biolearn.conf
