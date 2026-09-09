FROM devcontainer_debian_base:202608062122

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update && \
    apt-get install -y \
        openjdk-21-jdk \
        openjdk-25-jdk \
        && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Install Node.js LTS + npm
RUN curl -fsSL https://deb.nodesource.com/setup_lts.x | bash - && \
    apt-get update && \
    apt-get install -y nodejs && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Create non-root user
RUN useradd -ms /bin/bash dev 
#&& echo "vscode ALL=(ALL) NOPASSWD:ALL" >/etc/sudoers.d/dev
# uncomment for root in Kubernetes, in Docker use "docker exec --user root ..." 

USER dev

CMD ["/bin/bash"]
