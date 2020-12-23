#!/bin/bash
minikube --insecure-registry "registry-master.at4d.liacloud.com"  --host-only-cidr "192.168.64.2/24" --memory 6000 --cpus 2 start

echo "Building Dataquery..."
./gradlew installDist

echo "Setting Docker context to minikube environment..."

eval $(minikube docker-env)

echo "Enable ingress..."
minikube addons enable ingress

echo "Building image..."
export SERVICE_VERSION=$( awk '/^## \[([0-9])/{ print (substr($2, 2, length($2) - 2));exit; }' CHANGELOG.md )
docker build -t dm/dataquery:$SERVICE_VERSION .
echo "Taging image to latest..."
docker tag dm/dataquery:$SERVICE_VERSION dm/dataquery:latest

echo "Deploying service..."
kubectl config use-context minikube
kubectl delete --ignore-not-found=true -f ./K8sfile.yaml
kubectl apply -f ./K8sfile.yaml

echo "Update deployment"
kubectl apply -f minikube-env/k8s-dev-configuration.yaml

echo "Done."
