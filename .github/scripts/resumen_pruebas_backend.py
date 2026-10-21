"""
Nombre: resumen_pruebas_backend.py
Descripción: Lee los reportes de Surefire y el CSV de JaCoCo del backend y produce el resumen del
             pipeline de CI: tabla en el Job Summary, salidas del paso (total, fallidas, cobertura) y
             un JSON con las pruebas fallidas que usa el job reportar-fallas.
Fecha de creación: 2026-10-21
Última actualización: 2026-10-21
Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)

Uso (funciona en Windows y Linux):
    python .github/scripts/resumen_pruebas_backend.py \
        --surefire backend/target/surefire-reports \
        --jacoco backend/target/site/jacoco/jacoco.csv \
        --umbral 0.70 --salida-json resumen-backend.json
"""

import argparse
import csv
import glob
import json
import os
import sys
import xml.etree.ElementTree as ET

# Largo máximo del mensaje de cada falla, para que el issue siga siendo legible
LARGO_MAXIMO_MENSAJE = 600


def leer_surefire(carpeta):
    """Suma los contadores de todos los TEST-*.xml y recoge cada prueba fallida con su mensaje."""
    resumen = {"total": 0, "fallidas": 0, "errores": 0, "omitidas": 0, "fallas": [], "reportes": 0}
    for ruta in sorted(glob.glob(os.path.join(carpeta, "TEST-*.xml"))):
        try:
            suite = ET.parse(ruta).getroot()
        except ET.ParseError as error:
            print(f"Aviso: no se pudo leer {ruta}: {error}", file=sys.stderr)
            continue
        resumen["reportes"] += 1
        # Se cuentan los <testcase> uno a uno: con clases @Nested el atributo tests de la suite
        # puede quedarse corto respecto al total que informa Maven
        for caso in suite.iter("testcase"):
            resumen["total"] += 1
            if caso.find("skipped") is not None:
                resumen["omitidas"] += 1
            if caso.find("failure") is not None:
                resumen["fallidas"] += 1
            elif caso.find("error") is not None:
                resumen["errores"] += 1
            for tipo in ("failure", "error"):
                nodo = caso.find(tipo)
                if nodo is None:
                    continue
                # Sin atributo message se usa la primera línea de la traza
                mensaje = nodo.get("message") or ""
                if not mensaje:
                    traza = (nodo.text or "").strip()
                    mensaje = traza.splitlines()[0] if traza else ""
                mensaje = " ".join(mensaje.split())
                if len(mensaje) > LARGO_MAXIMO_MENSAJE:
                    mensaje = mensaje[:LARGO_MAXIMO_MENSAJE] + "…"
                resumen["fallas"].append({
                    "clase": caso.get("classname", ""),
                    "prueba": caso.get("name", ""),
                    "tipo": "falla" if tipo == "failure" else "error",
                    "excepcion": nodo.get("type", ""),
                    "mensaje": mensaje,
                })
    return resumen


def leer_jacoco(ruta):
    """Devuelve la cobertura de líneas y de ramas (0 a 1) del CSV de JaCoCo, o None si no existe."""
    if not os.path.isfile(ruta):
        return None
    lineas_faltantes = lineas_cubiertas = ramas_faltantes = ramas_cubiertas = 0
    with open(ruta, newline="", encoding="utf-8") as archivo:
        for fila in csv.DictReader(archivo):
            lineas_faltantes += int(fila["LINE_MISSED"])
            lineas_cubiertas += int(fila["LINE_COVERED"])
            ramas_faltantes += int(fila["BRANCH_MISSED"])
            ramas_cubiertas += int(fila["BRANCH_COVERED"])
    total_lineas = lineas_faltantes + lineas_cubiertas
    total_ramas = ramas_faltantes + ramas_cubiertas
    return {
        "lineas": lineas_cubiertas / total_lineas if total_lineas else 0.0,
        "ramas": ramas_cubiertas / total_ramas if total_ramas else 0.0,
    }


def celda(texto):
    """Escapa un texto para una celda de tabla Markdown."""
    return str(texto).replace("|", "\\|").replace("\n", " ")


def construir_markdown(pruebas, cobertura, umbral):
    """Arma la tabla del Job Summary y la lista de pruebas fallidas."""
    malas = pruebas["fallidas"] + pruebas["errores"]
    estado = "✅" if malas == 0 and pruebas["reportes"] > 0 else "❌"
    lineas = [f"## {estado} Backend: pruebas y cobertura", ""]
    if pruebas["reportes"] == 0:
        lineas += ["No se encontraron reportes de Surefire: la compilación falló antes de las pruebas.", ""]
    lineas += [
        "| Indicador | Valor |",
        "| --- | --- |",
        f"| Pruebas totales | {pruebas['total']} |",
        f"| Fallidas (incluye errores) | {malas} |",
        f"| Omitidas | {pruebas['omitidas']} |",
    ]
    if cobertura is None:
        lineas.append("| Cobertura de líneas | sin reporte de JaCoCo |")
    else:
        marca = "✅" if cobertura["lineas"] >= umbral else "❌"
        lineas.append(f"| Cobertura de líneas | {cobertura['lineas']:.1%} {marca} (mínimo {umbral:.0%}, RNF-MAN-001) |")
        lineas.append(f"| Cobertura de ramas | {cobertura['ramas']:.1%} |")
    if pruebas["fallas"]:
        lineas += ["", "### Pruebas fallidas", "", "| Clase | Prueba | Mensaje |", "| --- | --- | --- |"]
        for falla in pruebas["fallas"]:
            clase = falla["clase"].rsplit(".", 1)[-1]
            lineas.append(f"| {celda(clase)} | {celda(falla['prueba'])} | {celda(falla['mensaje'])} |")
    return "\n".join(lineas) + "\n"


def escribir_salidas(valores):
    """Publica las salidas del paso en GITHUB_OUTPUT cuando se ejecuta en GitHub Actions."""
    destino = os.environ.get("GITHUB_OUTPUT")
    if not destino:
        return
    with open(destino, "a", encoding="utf-8") as archivo:
        for clave, valor in valores.items():
            archivo.write(f"{clave}={valor}\n")


def main():
    """Punto de entrada: lee los reportes y escribe resumen, salidas y JSON."""
    parser = argparse.ArgumentParser(description="Resumen de pruebas y cobertura del backend")
    parser.add_argument("--surefire", required=True, help="Carpeta con los TEST-*.xml de Surefire")
    parser.add_argument("--jacoco", required=True, help="Ruta del jacoco.csv")
    parser.add_argument("--umbral", type=float, default=0.70, help="Cobertura mínima de líneas (0 a 1)")
    parser.add_argument("--salida-json", required=True, help="Archivo JSON con el resumen y las fallas")
    args = parser.parse_args()

    pruebas = leer_surefire(args.surefire)
    cobertura = leer_jacoco(args.jacoco)
    markdown = construir_markdown(pruebas, cobertura, args.umbral)

    resumen_paso = os.environ.get("GITHUB_STEP_SUMMARY")
    if resumen_paso:
        with open(resumen_paso, "a", encoding="utf-8") as archivo:
            archivo.write(markdown)
    else:
        sys.stdout.reconfigure(encoding="utf-8")
        print(markdown)

    datos = {
        "total": pruebas["total"],
        "fallidas": pruebas["fallidas"] + pruebas["errores"],
        "omitidas": pruebas["omitidas"],
        "reportes": pruebas["reportes"],
        "cobertura_lineas": None if cobertura is None else round(cobertura["lineas"], 4),
        "fallas": pruebas["fallas"],
    }
    with open(args.salida_json, "w", encoding="utf-8") as archivo:
        json.dump(datos, archivo, ensure_ascii=False, indent=2)

    escribir_salidas({
        "total": datos["total"],
        "fallidas": datos["fallidas"],
        "reportes": datos["reportes"],
        "cobertura": "" if cobertura is None else f"{cobertura['lineas']:.1%}",
    })


if __name__ == "__main__":
    main()
