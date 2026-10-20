# Oráculo independiente: simula el escenario con las reglas de negocio (RN) en Python,
# genera 02_datos_prueba.sql y calcula los valores esperados de las pruebas.
from decimal import Decimal as D, ROUND_HALF_UP
import json, datetime as dt

def r0(x):  # redondeo al entero, mitades hacia arriba (RN-06)
    return int(D(x).quantize(D(1), rounding=ROUND_HALF_UP))
def r4(x):
    return D(x).quantize(D('0.0001'), rounding=ROUND_HALF_UP)
def r50(x):  # redondeo a múltiplo de 50 (RN-07)
    return int((D(x) / 50).quantize(D(1), rounding=ROUND_HALF_UP)) * 50

def dv_nit(n):
    pesos = [3,7,13,17,19,23,29,37,41,43,47,53,59,67,71]
    s = sum(int(d) * pesos[i] for i, d in enumerate(reversed(n)))
    r = s % 11
    return r if r in (0, 1) else 11 - r
def ean13(d12):
    s = sum(int(c) * (1 if i % 2 == 0 else 3) for i, c in enumerate(d12))
    return d12 + str((10 - s % 10) % 10)

def q(v):
    if v is None: return 'NULL'
    if isinstance(v, bool): return 'true' if v else 'false'
    if isinstance(v, (int, D)): return str(v)
    if isinstance(v, dict): return "'" + json.dumps(v, ensure_ascii=False).replace("'", "''") + "'::jsonb"
    return "'" + str(v).replace("'", "''") + "'"

SQL = []
def ins(tabla, filas, override=True):
    if not filas: return
    cols = list(filas[0].keys())
    ov = ' OVERRIDING SYSTEM VALUE' if override and 'id' in cols else ''
    SQL.append(f"INSERT INTO {tabla} ({', '.join(cols)}){ov} VALUES\n  " +
               ",\n  ".join('(' + ', '.join(q(f[c]) for c in cols) + ')' for f in filas) + ';')
def raw(s): SQL.append(s)
def ts(fecha, hora='10:00'): return f'{fecha} {hora}:00-05'

# ---------------------------------------------------------------- maestros
USU = {'admin': 2, 'gerente': 3, 'jefe': 4, 'comprador': 5, 'vendedor': 6, 'auditor': 7, 'sistema': 1}
usuarios = [
    dict(id=2, usuario='admin',     nombre_completo='Ana Administradora', correo='admin@ferreteria.test'),
    dict(id=3, usuario='gerente',   nombre_completo='Gabriel Gerente',    correo='gerente@ferreteria.test'),
    dict(id=4, usuario='jefe',      nombre_completo='Julia Jefe Inventario', correo='jefe@ferreteria.test'),
    dict(id=5, usuario='comprador', nombre_completo='Carlos Comprador',   correo='compras@ferreteria.test'),
    dict(id=6, usuario='vendedor',  nombre_completo='Valeria Vendedora',  correo='ventas@ferreteria.test'),
    dict(id=7, usuario='auditor',   nombre_completo='Andrés Auditor',     correo='auditoria@ferreteria.test'),
]
for u in usuarios: u['password_hash'] = '$2b$12$hash.de.prueba.no.valido.para.login'
roles = {2: 'ADMIN', 3: 'GERENTE', 4: 'JEFE_INVENTARIO', 5: 'COMPRADOR', 6: 'VENDEDOR', 7: 'AUDITOR'}

EMP_NIT = '901234567'
empresa = dict(id=1, nit=EMP_NIT, digito_verificacion=dv_nit(EMP_NIT), razon_social='Ferretería Surcolombiana S.A.S.',
               direccion='Calle 8 # 5-20, Neiva', telefono='6088712345', correo='facturacion@ferreteria.test')
EMISOR = {k: empresa[k] for k in ('nit', 'digito_verificacion', 'razon_social', 'direccion', 'telefono', 'correo')}

marcas = ['Stanley', 'Centelsa', 'Pavco', 'Pintuco', 'Truper', 'Argos', 'Philips', 'Sika']
categorias = ['Herramientas', 'Eléctricos', 'Plomería', 'Pinturas', 'Tornillería', 'Construcción', 'Adhesivos y selladores']

terceros = []
def tercero(id, **k):
    t = dict(id=id, tipo_persona='JURIDICA', tipo_identificacion='NIT', numero_identificacion=None,
             digito_verificacion=None, nombre=None, apellido=None, razon_social=None, direccion='Neiva',
             telefono='3001234567', correo=None, es_cliente=False, es_proveedor=False)
    t.update(k)
    if t['tipo_identificacion'] == 'NIT': t['digito_verificacion'] = dv_nit(t['numero_identificacion'])
    terceros.append(t); return id
PAVCO = tercero(2, numero_identificacion='900123456', razon_social='Distribuidora Pavco Neiva S.A.S.', es_proveedor=True, correo='ventas@pavconeiva.test')
PINTU = tercero(3, numero_identificacion='830045678', razon_social='Pinturas del Huila S.A.S.', es_proveedor=True)
ANDINA = tercero(4, numero_identificacion='900987654', razon_social='Constructora Andina S.A.S.', es_cliente=True, correo='compras@andina.test', direccion='Carrera 5 # 10-30, Neiva')
JUAN = tercero(5, tipo_persona='NATURAL', tipo_identificacion='CC', numero_identificacion='1075234567', nombre='Juan', apellido='Pérez Gómez', es_cliente=True, telefono='3157654321')
FERRE = tercero(6, numero_identificacion='800555123', razon_social='Ferreconstrucciones Ltda.', es_cliente=True, es_proveedor=True)
CF = 1  # consumidor final (dato inicial del esquema)

# productos: id: (nombre, marca, categoria, unidad, pct, control, garantia)
PROD = {
    1: ('Martillo de uña 16 oz', 1, 1, 'UND', 30, 'GARANTIA', 6),
    2: ('Cable THHN 12 AWG', 2, 2, 'MTR', 25, 'NINGUNO', None),
    3: ('Tornillo drywall 6 x 1"', 5, 5, 'UND', 50, 'NINGUNO', None),
    4: ('Cemento gris uso general', 6, 6, 'KG', 15, 'NINGUNO', None),
    5: ('Bombillo LED 9 W', 7, 2, 'UND', 30, 'GARANTIA', 12),
    6: ('Silicona acética transparente 280 ml', 8, 7, 'UND', 40, 'VENCIMIENTO', None),
    7: ('Tubo PVC presión 1/2" x 6 m', 3, 3, 'UND', 35, 'NINGUNO', None),
    8: ('Soldadura líquida PVC 1/4 gal', 3, 3, 'UND', 35, 'VENCIMIENTO', None),
    9: ('Pintura vinilo tipo 1 blanco', 4, 4, 'GAL', 40, 'VENCIMIENTO', None),
}
# presentaciones: id: (producto, nombre, factor, fraccion, interno)
PRES = {
    1: (1, 'Unidad', 1, False, False), 2: (2, 'Metro', 1, True, True), 3: (2, 'Rollo 100 m', 100, False, False),
    4: (3, 'Unidad', 1, False, True), 5: (3, 'Caja x100', 100, False, False), 6: (4, 'Bulto 50 kg', 50, False, False),
    7: (4, 'Kilo', 1, True, True), 8: (5, 'Unidad', 1, False, False), 9: (6, 'Unidad', 1, False, False),
    10: (7, 'Unidad', 1, False, False), 11: (8, 'Unidad', 1, False, False), 12: (9, 'Galón', 1, False, False),
    13: (9, 'Cuñete 5 gal', 5, False, False),
}
def codigo(pid):
    if PRES[pid][4]: return ean13('20' + str(pid).zfill(10))
    return ean13('7702001' + str(pid).zfill(5))
IVA = 19

# ---------------------------------------------------------------- estado simulado
lotes = {}       # id -> dict
movs = []        # filas de movimiento_inventario
docs = []        # filas de documento
lineas = []      # filas de documento_linea
pagos = []
cambios = []
precios = []     # filas de precio_presentacion
precio_vig = {}  # presentacion -> fila vigente
ultimo_costo = {}
seq = {'lote': 0, 'mov': 0, 'doc': 0, 'lin': 0, 'pago': 0, 'cambio': 0, 'precio': 0}
num = {t: 0 for t in ('OC', 'DI', 'FV', 'NC', 'DP', 'AB', 'AJ', 'AP')}
def nid(k): seq[k] += 1; return seq[k]
def nnum(t): num[t] += 1; return num[t]

def nuevo_precio(pres, fecha, usuario, manual=None, motivo=None):
    prod = PRES[pres][0]; pct = PROD[prod][4]; factor = PRES[pres][2]
    if manual is None:
        precio = r0(ultimo_costo[prod] * factor * (1 + D(pct) / 100))
        if pres in precio_vig and precio_vig[pres]['origen'] == 'MANUAL':
            return  # RN-20: no se toca un precio manual
        if pres in precio_vig and precio_vig[pres]['precio_base'] == precio:
            return
    else:
        precio = manual
    if pres in precio_vig: precio_vig[pres]['vigente_hasta'] = fecha
    fila = dict(id=nid('precio'), presentacion_id=pres, precio_base=precio,
                origen='MANUAL' if manual else 'CALCULADO',
                costo_base_unitario=ultimo_costo[prod], porcentaje_incremento=D(pct),
                motivo=motivo, vigente_desde=fecha, vigente_hasta=None, usuario_id=usuario)
    precios.append(fila); precio_vig[pres] = fila

def doc(tipo, fecha, estado, creado, tercero=None, **k):
    d = dict(id=nid('doc'), tipo=tipo, numero=None, fecha=fecha, periodo=fecha[:7] + '-01', estado=estado,
             tercero_id=tercero, documento_origen_id=None, conteo_id=None, motivo_id=None,
             referencia_externa=None, observacion=None, subtotal=0, descuento=0, iva=0,
             ajuste_redondeo=0, total=0, emisor=EMISOR, creado_por=creado, creado_en=fecha,
             aprobado_por=None, aprobado_en=None)
    d.update(k)
    if estado not in ('BORRADOR', 'PROPUESTO', 'RECHAZADO'): d['numero'] = nnum(tipo)
    docs.append(d); return d

def linea(d, prod, pres=None, cantidad=None, cantidad_base=None, **k):
    factor = PRES[pres][2] if pres else 1
    l = dict(id=nid('lin'), documento_id=d['id'], numero_linea=sum(1 for x in lineas if x['documento_id'] == d['id']) + 1,
             producto_id=prod, presentacion_id=pres, lote_id=None, linea_origen_id=None, sentido=1,
             cantidad=D(cantidad), factor_conversion=D(factor),
             cantidad_base=D(cantidad_base) if cantidad_base is not None else D(cantidad) * factor,
             estado_inventario=None, costo_unitario=None, precio_unitario=None, descuento_pct=D(0),
             descuento_valor=0, tarifa_iva=D(0), iva_valor=0, subtotal=0, total=0, costo_total=None,
             fecha_vencimiento=None, fecha_fin_garantia=None, destino_baja=None, motivo_id=None)
    l.update(k); lineas.append(l); return l

def montos_linea(l, unitario, pct_desc=0):
    l['subtotal'] = r0(l['cantidad'] * unitario)
    l['descuento_pct'] = D(pct_desc)
    l['descuento_valor'] = r0(l['subtotal'] * D(pct_desc) / 100)
    l['tarifa_iva'] = D(IVA)
    l['iva_valor'] = r0((l['subtotal'] - l['descuento_valor']) * D(IVA) / 100)
    l['total'] = l['subtotal'] - l['descuento_valor'] + l['iva_valor']

def cerrar_montos(d, ajuste=0):
    ls = [l for l in lineas if l['documento_id'] == d['id']]
    d['subtotal'] = sum(l['subtotal'] for l in ls); d['descuento'] = sum(l['descuento_valor'] for l in ls)
    d['iva'] = sum(l['iva_valor'] for l in ls); d['ajuste_redondeo'] = ajuste
    d['total'] = d['subtotal'] - d['descuento'] + d['iva'] + ajuste

def mov(fecha, tipo, lote, cantidad, usuario, d=None, l=None, cambio=None, origen=None, destino=None):
    lt = lotes[lote]; cantidad = D(cantidad)
    m = dict(id=nid('mov'), fecha=fecha, periodo=fecha[:7] + '-01', tipo=tipo, producto_id=lt['producto_id'],
             lote_id=lote, documento_id=d['id'] if d else None, documento_linea_id=l['id'] if l else None,
             cambio_estado_id=cambio, estado_origen=origen, estado_destino=destino, cantidad=cantidad,
             costo_unitario_base=lt['costo_unitario_base'], costo_total=r0(cantidad * lt['costo_unitario_base']),
             usuario_id=usuario)
    col = {'DISPONIBLE': 'saldo_disponible', 'RESERVADA': 'saldo_reservado', 'DANADA': 'saldo_danado'}
    if origen: lt[col[origen]] -= cantidad
    if destino: lt[col[destino]] += cantidad
    assert lt['saldo_disponible'] >= 0 and lt['saldo_reservado'] >= 0 and lt['saldo_danado'] >= 0, (tipo, lote)
    movs.append(m); return m

def crear_lote(l, fecha, costo_base, venc=None):
    i = nid('lote')
    lotes[i] = dict(id=i, producto_id=l['producto_id'], documento_linea_id=l['id'], fecha_ingreso=fecha,
                    fecha_vencimiento=venc, costo_unitario_base=costo_base, cantidad_inicial=l['cantidad_base'],
                    saldo_disponible=D(0), saldo_reservado=D(0), saldo_danado=D(0))
    return i

def entrada(d, l, fecha, tipo, usuario, costo_base, venc=None):
    lote = crear_lote(l, fecha, costo_base, venc)
    l['fecha_vencimiento'] = venc
    mov(fecha, tipo, lote, l['cantidad_base'], usuario, d, l, destino='DISPONIBLE')
    prod = l['producto_id']; ultimo_costo[prod] = costo_base
    return lote

def lotes_vendibles(prod, fecha):
    hoy = fecha[:10]
    c = [x for x in lotes.values() if x['producto_id'] == prod and x['saldo_disponible'] > 0
         and (x['fecha_vencimiento'] is None or x['fecha_vencimiento'] >= hoy)]
    # RN-19: vencimiento (nulos al final), ingreso, id
    return sorted(c, key=lambda x: (x['fecha_vencimiento'] is None, x['fecha_vencimiento'] or '', x['fecha_ingreso'], x['id']))

def salida(d, l, fecha, tipo, usuario, origen='DISPONIBLE', desde_lotes=None):
    resto = l['cantidad_base']; costo = 0
    fuente = desde_lotes if desde_lotes else [(x['id'], None) for x in lotes_vendibles(l['producto_id'], fecha)]
    for lote, maximo in fuente:
        if resto == 0: break
        col = {'DISPONIBLE': 'saldo_disponible', 'RESERVADA': 'saldo_reservado', 'DANADA': 'saldo_danado'}[origen]
        disp = lotes[lote][col] if maximo is None else D(maximo)
        toma = min(disp, resto)
        if toma <= 0: continue
        m = mov(fecha, tipo, lote, toma, usuario, d, l, origen=origen)
        costo += m['costo_total']; resto -= toma
    assert resto == 0, ('existencia insuficiente', l)
    l['costo_total'] = costo
    return costo

def cambio_estado(fecha, lote, origen, destino, cantidad, usuario, motivo=None, reserva=None, autorizado=None):
    c = dict(id=nid('cambio'), lote_id=lote, estado_origen=origen, estado_destino=destino, cantidad=D(cantidad),
             motivo_id=motivo, reserva_id=reserva, usuario_id=usuario, autorizado_por=autorizado,
             fecha=fecha, periodo=fecha[:7] + '-01', observacion=None)
    cambios.append(c)
    mov(fecha, 'CAMBIO_ESTADO', lote, cantidad, usuario, cambio=c['id'], origen=origen, destino=destino)
    return c

def pagar(d, medios):
    """medios: lista de (medio, valor | 'resto', valor_recibido). El efectivo se redondea a 50 (RN-07)."""
    cerrar_montos(d)
    bruto = d['subtotal'] - d['descuento'] + d['iva']
    ajuste = 0; pagado = 0
    for medio, valor, recibido in medios:
        if valor == 'resto': valor = bruto - pagado
        if medio == 'EFECTIVO':
            redondeado = r50(valor); ajuste += redondeado - valor; valor = redondeado
        pagos.append(dict(id=nid('pago'), documento_id=d['id'], medio=medio, valor=valor,
                          valor_recibido=recibido if medio == 'EFECTIVO' else None,
                          cambio=(recibido - valor) if (medio == 'EFECTIVO' and recibido) else None,
                          referencia=None if medio == 'EFECTIVO' else 'REF-' + str(d['id'])))
        pagado += valor
    cerrar_montos(d, ajuste)
    assert pagado == d['total'], (pagado, d['total'])

def precio(pres): return precio_vig[pres]['precio_base']
def venta_linea(d, pres, cantidad, fecha, usuario, desc=0, desde_lotes=None, origen='DISPONIBLE'):
    prod = PRES[pres][0]
    l = linea(d, prod, pres, cantidad)
    l['precio_unitario'] = precio(pres)
    montos_linea(l, precio(pres), desc)
    if PROD[prod][5] == 'GARANTIA':
        f = dt.date.fromisoformat(fecha[:10]); meses = PROD[prod][6]
        y, m = divmod(f.month - 1 + meses, 12)
        l['fecha_fin_garantia'] = dt.date(f.year + y, m + 1, f.day).isoformat()
    salida(d, l, fecha, 'SALIDA_VENTA', usuario, origen=origen, desde_lotes=desde_lotes)
    return l

# motivos (ids según datos iniciales del esquema 01)
MOT = {'Rotura': 1, 'Defecto de fábrica': 2, 'Humedad': 3, 'Vencido': 4, 'Otro': 5, 'Diferencia de conteo': 6,
       'Error de registro': 7, 'Pérdida': 8, 'Hallazgo': 9, 'Producto inservible': 10, 'Vencido sin devolución': 11,
       'Cambio de opinión': 12, 'Defecto (cliente)': 13, 'Garantía': 14, 'Error de despacho': 15,
       'Defecto (proveedor)': 16, 'Error de pedido': 17, 'Vencimiento próximo': 18}

V, G, J, C, A, S = USU['vendedor'], USU['gerente'], USU['jefe'], USU['comprador'], USU['admin'], USU['sistema']

# ================================================================ AGOSTO 2026
# AP-1: migración inicial con costos (RN-29)
f = ts('2026-08-03', '07:00')
ap = doc('AP', f, 'CONFIRMADO', A, observacion='Migración inicial desde Excel')
apertura = [(1, 1, 20, 18000, None), (2, 2, 300, 2100, None), (3, 4, 2000, 60, None), (4, 7, 1000, 620, None),
            (5, 8, 50, 5500, None), (6, 9, 12, 9000, '2026-09-30')]
LOTE = {}
for prod, pres, cant, costo, venc in apertura:
    l = linea(ap, prod, pres, cant, costo_unitario=costo)
    LOTE[prod] = entrada(ap, l, f, 'APERTURA', A, r4(D(costo) / PRES[pres][2]), venc)
for pres in (1, 2, 3, 4, 5, 6, 7, 8, 9): nuevo_precio(pres, f, A)

# OC-1 a Pavco
f = ts('2026-08-05')
oc = doc('OC', f, 'EMITIDA', C, PAVCO)
oc_tubo = linea(oc, 7, 10, 100, costo_unitario=4000); montos_linea(oc_tubo, 4000)
oc_peg = linea(oc, 8, 11, 24, costo_unitario=18000); montos_linea(oc_peg, 18000)
cerrar_montos(oc)

# DI-1: recepción parcial de OC-1
f = ts('2026-08-08')
di1 = doc('DI', f, 'CONFIRMADO', C, PAVCO, documento_origen_id=oc['id'], referencia_externa='FE-PAV-1201')
l = linea(di1, 7, 10, 60, costo_unitario=4000, linea_origen_id=oc_tubo['id']); montos_linea(l, 4000)
LOTE['tubo1'] = entrada(di1, l, f, 'ENTRADA_COMPRA', C, r4(4000))
l = linea(di1, 8, 11, 24, costo_unitario=18000, linea_origen_id=oc_peg['id']); montos_linea(l, 18000)
LOTE['peg'] = entrada(di1, l, f, 'ENTRADA_COMPRA', C, r4(18000), '2027-02-28')
di1_peg_linea = l
cerrar_montos(di1)
for pres in (10, 11): nuevo_precio(pres, f, C)
OC_ESTADOS = [('2026-08-08', 'RECIBIDA_PARCIAL')]

# DI-2: compra directa de pintura, lote A (vence 2027-06-30)
f = ts('2026-08-10')
di2 = doc('DI', f, 'CONFIRMADO', C, PINTU, referencia_externa='FV-PH-889')
l = linea(di2, 9, 12, 10, costo_unitario=45000); montos_linea(l, 45000)
LOTE['pintA'] = entrada(di2, l, f, 'ENTRADA_COMPRA', C, r4(45000), '2027-06-30')
cerrar_montos(di2)
for pres in (12, 13): nuevo_precio(pres, f, C)

# FV-1: consumidor final, efectivo con redondeo
f = ts('2026-08-12', '11:15')
fv1 = doc('FV', f, 'CONFIRMADO', V, CF)
fv1_martillo = venta_linea(fv1, 1, 2, f, V)
venta_linea(fv1, 2, 15, f, V)
venta_linea(fv1, 7, D('2.5'), f, V)
pagar(fv1, [('EFECTIVO', 'resto', 110000)])

# FV-2: Constructora Andina, tarjeta de crédito, descuento 5 % en cemento
f = ts('2026-08-20', '15:40')
fv2 = doc('FV', f, 'CONFIRMADO', V, ANDINA)
venta_linea(fv2, 13, 1, f, V)
venta_linea(fv2, 5, 1, f, V)
venta_linea(fv2, 6, 4, f, V, desc=5)
venta_linea(fv2, 9, 4, f, V)
pagar(fv2, [('CREDITO', 'resto', None)])

# Cambio de estado: 1 martillo dañado (rotura)
f = ts('2026-08-25', '09:00')
c_martillo = cambio_estado(f, LOTE[1], 'DISPONIBLE', 'DANADA', 1, J, MOT['Rotura'])

# Conteo total de agosto: faltan 5 tornillos
f_conteo = ts('2026-08-31', '18:00')
conteo_lineas = []
for x in sorted(lotes.values(), key=lambda x: x['id']):
    prod = x['producto_id']
    por_lote = PROD[prod][5] == 'VENCIMIENTO'
    reg_b = x['saldo_disponible'] + x['saldo_reservado']; reg_d = x['saldo_danado']
    cont_b = reg_b - (5 if prod == 3 else 0)
    conteo_lineas.append(dict(id=len(conteo_lineas) + 1, conteo_id=1, producto_id=prod,
                              lote_id=x['id'] if por_lote else None, contado_buen_estado=cont_b,
                              contado_danado=reg_d, registrado_buen_estado=reg_b, registrado_danado=reg_d,
                              capturado_por=J, capturado_en=f_conteo, ajuste_linea_id=None))
aj1 = doc('AJ', ts('2026-08-31', '19:00'), 'APROBADO', J, conteo_id=1, motivo_id=MOT['Diferencia de conteo'],
          aprobado_por=G, aprobado_en=ts('2026-08-31', '19:30'))
l = linea(aj1, 3, 4, 5, sentido=-1, lote_id=LOTE[3], estado_inventario='DISPONIBLE', motivo_id=MOT['Diferencia de conteo'])
salida(aj1, l, ts('2026-08-31', '19:30'), 'AJUSTE_NEGATIVO', G, desde_lotes=[(LOTE[3], None)])
for cl in conteo_lineas:
    if cl['producto_id'] == 3: cl['ajuste_linea_id'] = l['id']

# Saldos al cierre de agosto
cierre_ago = [dict(periodo='2026-08-01', lote_id=x['id'], saldo_disponible=x['saldo_disponible'],
                   saldo_reservado=x['saldo_reservado'], saldo_danado=x['saldo_danado'],
                   costo_unitario_base=x['costo_unitario_base'],
                   valor=r0((x['saldo_disponible'] + x['saldo_reservado'] + x['saldo_danado']) * x['costo_unitario_base']))
              for x in sorted(lotes.values(), key=lambda x: x['id'])]
CORTE_AGO = len(docs), len(movs), len(lineas), len(pagos), len(cambios), len(precios)

# ================================================================ SEPTIEMBRE 2026
# Precio manual del bombillo (gerente)
nuevo_precio(8, ts('2026-09-01', '08:00'), G, manual=7900, motivo='Ajuste a precio de mercado')

# DI-3: resto de OC-1, el tubo sube a 4.200
f = ts('2026-09-02')
di3 = doc('DI', f, 'CONFIRMADO', C, PAVCO, documento_origen_id=oc['id'], referencia_externa='FE-PAV-1288')
l = linea(di3, 7, 10, 40, costo_unitario=4200, linea_origen_id=oc_tubo['id']); montos_linea(l, 4200)
LOTE['tubo2'] = entrada(di3, l, f, 'ENTRADA_COMPRA', C, r4(4200))
cerrar_montos(di3)
nuevo_precio(10, f, C)
OC_ESTADOS.append(('2026-09-02', 'RECIBIDA'))

# DI-4: pintura lote B, más cara y vence ANTES que el lote A (prueba FEFO)
f = ts('2026-09-03')
di4 = doc('DI', f, 'CONFIRMADO', C, PINTU, referencia_externa='FV-PH-951')
l = linea(di4, 9, 12, 10, costo_unitario=47000); montos_linea(l, 47000)
LOTE['pintB'] = entrada(di4, l, f, 'ENTRADA_COMPRA', C, r4(47000), '2027-03-31')
cerrar_montos(di4)
for pres in (12, 13): nuevo_precio(pres, f, C)

# FV-3: Juan Pérez, 8 galones, pago mixto débito + efectivo
f = ts('2026-09-05', '16:20')
fv3 = doc('FV', f, 'CONFIRMADO', V, JUAN)
fv3_pint = venta_linea(fv3, 12, 8, f, V)
bruto = sum(x['total'] for x in lineas if x['documento_id'] == fv3['id'])
pagar(fv3, [('DEBITO', 400000, None), ('EFECTIVO', 'resto', 230000)])

# NC-1: devolución de 1 martillo de FV-1, en buen estado, reintegro en efectivo
f = ts('2026-09-10', '10:30')
nc1 = doc('NC', f, 'CONFIRMADO', V, CF, documento_origen_id=fv1['id'], motivo_id=MOT['Cambio de opinión'])
l = linea(nc1, 1, 1, 1, linea_origen_id=fv1_martillo['id'], estado_inventario='DISPONIBLE',
          precio_unitario=fv1_martillo['precio_unitario'], motivo_id=MOT['Cambio de opinión'])
montos_linea(l, fv1_martillo['precio_unitario'])
m_orig = [m for m in movs if m['documento_linea_id'] == fv1_martillo['id']][0]
m = mov(f, 'DEVOLUCION_CLIENTE', m_orig['lote_id'], 1, V, nc1, l, destino='DISPONIBLE')
l['costo_total'] = m['costo_total']
pagar(nc1, [('EFECTIVO', 'resto', None)])

# Reserva de 10 tubos para Constructora Andina (vence a los 7 días)
f = ts('2026-09-12', '09:00')
reserva1 = dict(id=1, cliente_id=ANDINA, presentacion_id=10, producto_id=7, cantidad=D(10), cantidad_base=D(10),
                fecha_limite='2026-09-19', estado='CONSUMIDA', factura_id=None, creado_por=V, creado_en=f,
                cerrado_en=None)
lote_res = lotes_vendibles(7, f)[0]['id']
reserva_lote = [dict(reserva_id=1, lote_id=lote_res, cantidad=D(10))]
cambio_estado(f, lote_res, 'DISPONIBLE', 'RESERVADA', 10, V, reserva=1)

# DP-1: devolución de 2 soldaduras defectuosas a Pavco (desde DI-1)
f = ts('2026-09-15')
dp1 = doc('DP', f, 'CONFIRMADO', C, PAVCO, documento_origen_id=di1['id'], motivo_id=MOT['Defecto (proveedor)'])
l = linea(dp1, 8, 11, 2, lote_id=LOTE['peg'], estado_inventario='DISPONIBLE', costo_unitario=18000,
          linea_origen_id=di1_peg_linea['id'], motivo_id=MOT['Defecto (proveedor)'])
montos_linea(l, 18000)
salida(dp1, l, f, 'DEVOLUCION_PROVEEDOR', C, desde_lotes=[(LOTE['peg'], None)])
cerrar_montos(dp1)

# FV-4: Andina compra los 10 tubos reservados, transferencia
f = ts('2026-09-16', '14:00')
fv4 = doc('FV', f, 'CONFIRMADO', V, ANDINA)
venta_linea(fv4, 10, 10, f, V, desde_lotes=[(lote_res, 10)], origen='RESERVADA')
pagar(fv4, [('TRANSFERENCIA', 'resto', None)])
reserva1['factura_id'] = fv4['id']; reserva1['cerrado_en'] = f

# AB-1: baja del martillo dañado, propuesta por la jefe, aprobada por el gerente
ab1 = doc('AB', ts('2026-09-20'), 'APROBADO', J, motivo_id=MOT['Producto inservible'],
          aprobado_por=G, aprobado_en=ts('2026-09-21', '08:30'))
l = linea(ab1, 1, 1, 1, lote_id=LOTE[1], estado_inventario='DANADA', destino_baja='RECICLAJE',
          motivo_id=MOT['Producto inservible'])
salida(ab1, l, ts('2026-09-21', '08:30'), 'BAJA', G, origen='DANADA', desde_lotes=[(LOTE[1], None)])

# ================================================================ OCTUBRE 2026
# Tarea diaria del Sistema: la silicona venció el 30/09 y pasa a dañada (RN-18)
f = ts('2026-10-01', '00:05')
sil = lotes[LOTE[6]]
cambio_estado(f, LOTE[6], 'DISPONIBLE', 'DANADA', sil['saldo_disponible'], S, MOT['Vencido'])

# FV-5: 3 bombillos a precio manual, transferencia, garantía 12 meses
f = ts('2026-10-05', '12:00')
fv5 = doc('FV', f, 'CONFIRMADO', V, JUAN)
venta_linea(fv5, 8, 3, f, V)
pagar(fv5, [('TRANSFERENCIA', 'resto', None)])

# Reserva activa: 2 bombillos para Ferreconstrucciones
f = ts('2026-10-15', '09:30')
reserva2 = dict(id=2, cliente_id=FERRE, presentacion_id=8, producto_id=5, cantidad=D(2), cantidad_base=D(2),
                fecha_limite='2026-10-22', estado='ACTIVA', factura_id=None, creado_por=V, creado_en=f, cerrado_en=None)
reserva_lote.append(dict(reserva_id=2, lote_id=LOTE[5], cantidad=D(2)))
cambio_estado(f, LOTE[5], 'DISPONIBLE', 'RESERVADA', 2, V, reserva=2)

# ultimo costo en producto
# ================================================================ EMISIÓN DEL SQL
raw("""-- =====================================================================
-- 02_datos_prueba.sql — Escenario de prueba agosto–octubre 2026
-- Generado por el oráculo (oraculo.py). Requiere 01_esquema_inventario_ferreteria.sql
-- Ejecución: psql -U postgres -d ferreteria -v ON_ERROR_STOP=1 -f 02_datos_prueba.sql
-- =====================================================================
BEGIN;
SET LOCAL app.usuario_id = '2';
SET LOCAL app.ip = '127.0.0.1';
""")
ins('usuario', usuarios)
raw("INSERT INTO usuario_rol (usuario_id, rol_id)\nSELECT v.u, r.id FROM (VALUES " +
    ", ".join(f"({u}, '{c}')" for u, c in roles.items()) + ") AS v(u, c) JOIN rol r ON r.codigo = v.c;")
ins('empresa', [empresa], override=False)
ins('marca', [dict(id=i + 1, nombre=n) for i, n in enumerate(marcas)])
ins('categoria', [dict(id=i + 1, nombre=n) for i, n in enumerate(categorias)])
ins('producto', [dict(id=i, nombre=p[0], marca_id=p[1], categoria_id=p[2], unidad_base=p[3], tarifa_iva_id=3,
                      porcentaje_incremento=D(p[4]), control=p[5], plazo_garantia_meses=p[6])
                 for i, p in PROD.items()])
ins('presentacion', [dict(id=i, producto_id=p[0], nombre=p[1], codigo_barras=codigo(i), codigo_interno=p[4],
                          factor_conversion=D(p[2]), admite_fraccion=p[3]) for i, p in PRES.items()])
ins('tercero', terceros)

DOC_COLS = ['id', 'tipo', 'numero', 'fecha', 'periodo', 'estado', 'tercero_id', 'documento_origen_id', 'conteo_id',
            'motivo_id', 'referencia_externa', 'observacion', 'subtotal', 'descuento', 'iva', 'ajuste_redondeo',
            'total', 'emisor', 'creado_por', 'creado_en', 'aprobado_por', 'aprobado_en']
def emitir_bloque(ds, ls, ms, ps, cs, prs, lote_ids):
    if prs: ins('precio_presentacion', prs)
    for d in ds:
        ins('documento', [{k: d[k] for k in DOC_COLS}])
        ins('documento_linea', [x for x in ls if x['documento_id'] == d['id']])
        lids = [lt for lt in lote_ids if lotes[lt]['documento_linea_id'] in [x['id'] for x in ls if x['documento_id'] == d['id']]]
        if lids: ins('lote', [lotes_final[i] for i in lids])
        ins('movimiento_inventario', [m for m in ms if m['documento_id'] == d['id']])
        ins('pago', [p for p in ps if p['documento_id'] == d['id']])
    for c in cs:
        ins('cambio_estado', [c])
        ins('movimiento_inventario', [m for m in ms if m['cambio_estado_id'] == c['id']])

lotes_final = {i: dict(x) for i, x in lotes.items()}
docs_ago = [d for d in docs if d['fecha'] < '2026-09']
lotes_ago = [i for i, x in lotes.items() if x['fecha_ingreso'] < '2026-09']
raw("\n-- ---------------- AGOSTO 2026 ----------------")
# el conteo debe existir antes del AJ que lo referencia
pre_aj = [d for d in docs_ago if d['tipo'] != 'AJ']
emitir_bloque(pre_aj, lineas, movs, pagos, [c for c in cambios if c['fecha'] < '2026-09'],
              [p for p in precios if p['vigente_desde'] < '2026-09'], lotes_ago)
raw("UPDATE documento SET estado = 'RECIBIDA_PARCIAL' WHERE id = %d;  -- OC-1 tras DI-1" % oc['id'])
ins('conteo', [dict(id=1, tipo='TOTAL', estado='CERRADO', alcance=None, periodo='2026-08-01', iniciado_por=J,
                    iniciado_en=ts('2026-08-31', '17:00'), cerrado_en=f_conteo, observacion='Conteo total de cierre de agosto')])
emitir_bloque([aj1], lineas, movs, pagos, [], [], [])
ins('conteo_linea', conteo_lineas)
ins('cierre_saldo', cierre_ago, override=False)
raw("""UPDATE periodo SET estado = 'CERRADO', conteo_id = 1, cerrado_por = 3, cerrado_en = '2026-09-01 07:30:00-05'
 WHERE periodo = '2026-08-01';
INSERT INTO periodo_evento (periodo, accion, usuario_id, fecha) VALUES ('2026-08-01', 'CIERRE', 3, '2026-09-01 07:30:00-05');""")

raw("\n-- ---------------- SEPTIEMBRE Y OCTUBRE 2026 ----------------")
ins('reserva', [dict(reserva1, estado='ACTIVA', factura_id=None, cerrado_en=None), reserva2])
ins('reserva_lote', reserva_lote, override=False)
docs_sep = [d for d in docs if d['fecha'] >= '2026-09']
emitir_bloque(docs_sep, lineas, movs, pagos, [c for c in cambios if c['fecha'] >= '2026-09'],
              [p for p in precios if p['vigente_desde'] >= '2026-09'], [i for i in lotes if i not in lotes_ago])
raw("UPDATE documento SET estado = 'RECIBIDA' WHERE id = %d;  -- OC-1 tras DI-3" % oc['id'])
raw(f"UPDATE reserva SET estado = 'CONSUMIDA', factura_id = {fv4['id']}, cerrado_en = '{reserva1['cerrado_en']}' WHERE id = 1;")
raw("UPDATE precio_presentacion SET vigente_hasta = v.hasta FROM (VALUES " +
    ", ".join(f"({p['id']}, '{p['vigente_hasta']}'::timestamptz)" for p in precios if p['vigente_hasta']) +
    ") AS v(id, hasta) WHERE precio_presentacion.id = v.id;")
raw("UPDATE producto SET ultimo_costo_base = v.c FROM (VALUES " +
    ", ".join(f"({p}, {c})" for p, c in sorted(ultimo_costo.items())) + ") AS v(id, c) WHERE producto.id = v.id;")
raw("\n-- Consecutivos y secuencias de identidad")
raw("UPDATE consecutivo SET siguiente_numero = v.n FROM (VALUES " +
    ", ".join(f"('{t}', {n + 1})" for t, n in num.items()) + ") AS v(t, n) WHERE tipo_documento = v.t;")
for t in ['usuario', 'marca', 'categoria', 'producto', 'presentacion', 'tercero', 'precio_presentacion', 'documento',
          'documento_linea', 'pago', 'lote', 'reserva', 'cambio_estado', 'movimiento_inventario', 'conteo', 'conteo_linea']:
    raw(f"SELECT setval(pg_get_serial_sequence('{t}', 'id'), (SELECT max(id) FROM {t}));")
raw("\nCOMMIT;")

# precios vigentes: los inserts de precio se hacen con vigente_hasta NULL y luego se cierran
for p in precios: pass

# ---------------------------------------------------------------- valores esperados
def ventas_periodo(ini, fin):
    fv = [d for d in docs if d['tipo'] == 'FV' and ini <= d['fecha'][:10] <= fin]
    nc = [d for d in docs if d['tipo'] == 'NC' and ini <= d['fecha'][:10] <= fin]
    neto = lambda ds: sum(l['subtotal'] - l['descuento_valor'] for d in ds for l in lineas if l['documento_id'] == d['id'])
    costo = lambda ds: sum(l['costo_total'] for d in ds for l in lineas if l['documento_id'] == d['id'])
    iva = lambda ds: sum(l['iva_valor'] for d in ds for l in lineas if l['documento_id'] == d['id'])
    return dict(facturas=len(fv), venta_neta=neto(fv) - neto(nc), costo=costo(fv) - costo(nc),
                utilidad=(neto(fv) - neto(nc)) - (costo(fv) - costo(nc)), iva=iva(fv) - iva(nc),
                total_facturado=sum(d['total'] for d in fv))

def existencias_al(corte):
    lim = corte + ' 23:59:59.999-05'
    res = {}
    for m in movs:
        if m['fecha'] > lim: continue
        s = (1 if m['estado_destino'] else 0) - (1 if m['estado_origen'] else 0)
        r = res.setdefault(m['producto_id'], {'total': D(0), 'valor_lotes': {}})
        r['total'] += s * m['cantidad']
        lt = r['valor_lotes'].setdefault(m['lote_id'], D(0)); r['valor_lotes'][m['lote_id']] = lt + s * m['cantidad']
    out = {}
    for p, r in res.items():
        valor = r0(sum(q_ * lotes[l]['costo_unitario_base'] for l, q_ in r['valor_lotes'].items()))
        out[p] = dict(total=r['total'], valor=valor)
    return out

def compras_periodo(ini, fin):
    di = [d for d in docs if d['tipo'] == 'DI' and ini <= d['fecha'][:10] <= fin]
    dp = [d for d in docs if d['tipo'] == 'DP' and ini <= d['fecha'][:10] <= fin]
    return dict(documentos=len(di), subtotal=sum(d['subtotal'] for d in di), total=sum(d['total'] for d in di),
                devoluciones=sum(d['subtotal'] for d in dp), neto=sum(d['subtotal'] for d in di) - sum(d['subtotal'] for d in dp))

ex_ago = existencias_al('2026-08-31'); ex_hoy = existencias_al('2026-10-20')
esperado = dict(
    ventas_ago=ventas_periodo('2026-08-01', '2026-08-31'), ventas_sep=ventas_periodo('2026-09-01', '2026-09-30'),
    ventas_oct=ventas_periodo('2026-10-01', '2026-10-31'),
    compras_ago=compras_periodo('2026-08-01', '2026-08-31'), compras_sep=compras_periodo('2026-09-01', '2026-09-30'),
    valor_inventario_ago=sum(v['valor'] for v in ex_ago.values()),
    valor_cierre_ago=sum(c['valor'] for c in cierre_ago),
    valor_inventario_hoy=sum(v['valor'] for v in ex_hoy.values()),
    existencias_hoy={p: dict(total=str(v['total']), valor=v['valor']) for p, v in ex_hoy.items()},
    fv1=dict((k, fv1[k]) for k in ('subtotal', 'iva', 'ajuste_redondeo', 'total')),
    fv1_cambio=[p['cambio'] for p in pagos if p['documento_id'] == fv1['id']][0],
    fv2=dict((k, fv2[k]) for k in ('subtotal', 'descuento', 'iva', 'total')),
    fv3=dict((k, fv3[k]) for k in ('subtotal', 'iva', 'ajuste_redondeo', 'total')),
    fv3_costo=fv3_pint['costo_total'], fv3_lotes=[(m['lote_id'], str(m['cantidad'])) for m in movs if m['documento_linea_id'] == fv3_pint['id']],
    nc1=dict((k, nc1[k]) for k in ('subtotal', 'iva', 'ajuste_redondeo', 'total')),
    precio_tubo=precio_vig[10]['precio_base'], precio_tubo_iva=r0(precio_vig[10]['precio_base'] * D('1.19')),
    precio_bombillo=precio_vig[8]['precio_base'], precio_galon=precio_vig[12]['precio_base'],
    precio_cunete=precio_vig[13]['precio_base'], historial_tubo=len([p for p in precios if p['presentacion_id'] == 10]),
    lote_pintA=LOTE['pintA'], lote_pintB=LOTE['pintB'], lote_silicona=LOTE[6], lote_tubo1=LOTE['tubo1'],
    saldos={i: {k: str(x[k]) for k in ('saldo_disponible', 'saldo_reservado', 'saldo_danado')} for i, x in lotes.items()},
    danado_sep=dict(cambios=[(c['lote_id'], str(c['cantidad'])) for c in cambios if c['estado_destino'] == 'DANADA']),
    baja_valor=sum(m['costo_total'] for m in movs if m['tipo'] == 'BAJA'),
    dif_conteo_valor=r0(5 * lotes[LOTE[3]]['costo_unitario_base']),
    iva_ago=ventas_periodo('2026-08-01', '2026-08-31')['iva'],
    n=dict(docs=len(docs), lineas=len(lineas), movs=len(movs), lotes=len(lotes), pagos=len(pagos), cambios=len(cambios),
           precios=len(precios)),
    num=num, codigo_metro=codigo(2), codigo_martillo=codigo(1),
    garantia_fv5=[l['fecha_fin_garantia'] for l in lineas if l['documento_id'] == fv5['id']][0],
)
open('esperado.json', 'w').write(json.dumps(esperado, indent=1, default=str, ensure_ascii=False))
open('02_datos_prueba.sql', 'w').write('\n'.join(SQL) + '\n')
print(json.dumps(esperado, indent=1, default=str, ensure_ascii=False))
