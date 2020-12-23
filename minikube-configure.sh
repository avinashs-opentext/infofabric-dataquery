#!/bin/bash

echo "Applying minikube environment configuration..."

cd minikube-env

kubectl config use-context minikube

kubectl delete --ignore-not-found=true -f k8s-dev-configuration.yaml
kubectl delete --ignore-not-found=true -f k8s-dev-pvs.yaml
kubectl delete --ignore-not-found=true -f k8s-dev-metrics.yaml

kubectl apply -f k8s-dev-configuration.yaml
kubectl apply -f k8s-dev-pvs.yaml
kubectl apply -f k8s-dev-metrics.yaml

echo "Done."
