{{/*
Expand the name of the chart.
*/}}
{{- define "carnetroute.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
*/}}
{{- define "carnetroute.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart label.
*/}}
{{- define "carnetroute.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels.
*/}}
{{- define "carnetroute.labels" -}}
helm.sh/chart: {{ include "carnetroute.chart" . }}
{{ include "carnetroute.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels.
*/}}
{{- define "carnetroute.selectorLabels" -}}
app.kubernetes.io/name: {{ include "carnetroute.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Namespace helper.
*/}}
{{- define "carnetroute.namespace" -}}
{{- default "carnetroute" .Values.namespace }}
{{- end }}

{{/*
Backend image.
*/}}
{{- define "carnetroute.backendImage" -}}
{{- printf "%s/%s/%s:%s" .Values.image.registry .Values.image.repository .Values.backend.image.name .Values.backend.image.tag }}
{{- end }}

{{/*
Frontend image.
*/}}
{{- define "carnetroute.frontendImage" -}}
{{- printf "%s/%s/%s:%s" .Values.image.registry .Values.image.repository .Values.frontend.image.name .Values.frontend.image.tag }}
{{- end }}
