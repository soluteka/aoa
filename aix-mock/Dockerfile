FROM ubuntu:22.04

# Instalar openssh-server
RUN apt-get update && \
    apt-get install -y --no-install-recommends openssh-server && \
    rm -rf /var/lib/apt/lists/* && \
    mkdir -p /var/run/sshd

# Crear usuario aixmock sin password (login solo por llave)
RUN useradd -m -s /bin/bash aixmock && \
    passwd -d aixmock && \
    mkdir -p /home/aixmock/.ssh && \
    chmod 700 /home/aixmock/.ssh

# Copiar script mock que simula lparstat -X
COPY lparstat-mock.sh /usr/local/bin/lparstat
RUN chmod +x /usr/local/bin/lparstat && \
    chown root:root /usr/local/bin/lparstat

# Copiar llave pública (la generaremos en paso 2)
COPY authorized_keys /home/aixmock/.ssh/authorized_keys
RUN chmod 600 /home/aixmock/.ssh/authorized_keys && \
    chown -R aixmock:aixmock /home/aixmock/.ssh

# Endurecer sshd: solo llave, sin password
RUN sed -i 's/#PasswordAuthentication yes/PasswordAuthentication no/' /etc/ssh/sshd_config && \
    sed -i 's/#PubkeyAuthentication yes/PubkeyAuthentication yes/' /etc/ssh/sshd_config && \
    echo "PermitRootLogin no" >> /etc/ssh/sshd_config

EXPOSE 22

CMD ["/usr/sbin/sshd", "-D", "-e"]