#!/bin/bash

echo "Re-deploying service..."

kubectl config use-context minikube

kubectl apply -f minikube-env/k8s-dev-configuration.yaml

kubectl delete --ignore-not-found=true -f K8sfile.yaml
kubectl apply -f K8sfile.yaml

echo "Done."
