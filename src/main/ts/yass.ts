// $note: line below and corresponding file could probably be removed with TypeScript > 1.4
/// <reference path="es6-promise"/>

module yass {

    export class Writer {
        private capacity: number;
        private position = 0;
        array: Uint8Array;
        constructor(initialCapacity: number) {
            this.capacity = initialCapacity;
            this.array = new Uint8Array(initialCapacity);
        }
        needed(value: number): number {
            var oldPosition = this.position;
            this.position += value;
            if (this.position > this.capacity) {
                var oldArray = this.array;
                this.capacity = 2 * this.position;
                this.array = new Uint8Array(this.capacity);
                this.array.set(oldArray);
            }
            return oldPosition;
        }
        writeByte(value: number): void {
            var position = this.needed(1);
            this.array[position] = value;
        }
        writeInt(value: number): void {
            var position = this.needed(4);
            new DataView(this.array.buffer).setInt32(position, value);
        }
        writeVarInt(value: number): void {
            while (true) {
                if ((value & ~0x7F) === 0) {
                    this.writeByte(value);
                    return;
                }
                this.writeByte((value & 0x7F) | 0x80);
                value >>>= 7;
            }
        }
        writeZigZagInt(value: number): void {
            this.writeVarInt((value << 1) ^ (value >> 31));
        }
        static calcUtf8bytes(value: string): number {
            var bytes = 0;
            for (var c = 0; c < value.length; c++) {
                var code = value.charCodeAt(c);
                if (code < 0x80) {
                    bytes += 1;
                } else if (code < 0x800) {
                    bytes += 2;
                } else {
                    bytes += 3;
                }
            }
            return bytes;
        }
        writeUtf8(value: string): void {
            for (var c = 0; c < value.length; c++) {
                var code = value.charCodeAt(c);
                if (code < 0x80) { // 0xxx xxxx
                    this.writeByte(code);
                } else if (code < 0x800) { // 110x xxxx  10xx xxxx
                    this.writeByte(0xC0 | ((code >> 6) & 0x1F));
                    this.writeByte(0x80 | (code & 0x3F));
                } else { // 1110 xxxx  10xx xxxx  10xx xxxx
                    this.writeByte(0xE0 | ((code >> 12) & 0x0F));
                    this.writeByte(0x80 | ((code >> 6) & 0x3F));
                    this.writeByte(0x80 | (code & 0x3F));
                }
            }
        }
        getArray(): Uint8Array {
            return this.array.subarray(0, this.position);
        }
    }

    export class Reader {
        array: Uint8Array;
        private length: number;
        private position = 0;
        constructor(arrayBuffer: ArrayBuffer) {
            this.array = new Uint8Array(arrayBuffer);
            this.length = arrayBuffer.byteLength;
        }
        needed(value: number): number {
            var oldPosition = this.position;
            this.position += value;
            if (this.position > this.length) {
                throw new Error("reader buffer underflow");
            }
            return oldPosition;
        }
        isEmpty(): boolean {
            return this.position >= this.length;
        }
        readByte(): number {
            return this.array[this.needed(1)];
        }
        readInt(): number {
            return new DataView(this.array.buffer).getInt32(this.needed(4));
        }
        readVarInt(): number {
            var shift = 0;
            var value = 0;
            while (shift < 32) {
                var b = this.readByte();
                value |= (b & 0x7F) << shift;
                if ((b & 0x80) === 0) {
                    return value;
                }
                shift += 7;
            }
            throw new Error("malformed VarInt input");
        }
        readZigZagInt(): number {
            var value = this.readVarInt();
            return (value >>> 1) ^ -(value & 1);
        }
        readUtf8(bytes: number): string {
            var result = "";
            while (bytes-- > 0) {
                var code: number;
                var b1 = this.readByte();
                if ((b1 & 0x80) === 0) { // 0xxx xxxx
                    code = b1;
                } else if ((b1 & 0xE0) === 0xC0) { // 110x xxxx  10xx xxxx
                    var b2 = this.readByte();
                    if ((b2 & 0xC0) !== 0x80) {
                        throw new Error("malformed String input (1)");
                    }
                    code = ((b1 & 0x1F) << 6) | (b2 & 0x3F);
                    bytes--;
                } else if ((b1 & 0xF0) === 0xE0) { // 1110 xxxx  10xx xxxx  10xx xxxx
                    var b2 = this.readByte();
                    var b3 = this.readByte();
                    if (((b2 & 0xC0) !== 0x80) || ((b3 & 0xC0) !== 0x80)) {
                        throw new Error("malformed String input (2)");
                    }
                    code = ((b1 & 0x0F) << 12) | ((b2 & 0x3F) << 6) | (b3 & 0x3F);
                    bytes -= 2;
                } else {
                    throw new Error("malformed String input (3)");
                }
                result += String.fromCharCode(code);
            }
            return result;
        }
    }

    export class Type {
        // empty
    }

    export class Enum extends Type {
        constructor(public number: number, public name: string) {
            super();
        }
        toString(): string {
            return this.name;
        }
    }

    export interface TypeHandler<T> {
        read (reader: Reader, id2typeHandler?: TypeHandler<any>[]): T;
        write(value: T, writer: Writer): void;
    }

    export class TypeDesc {
        constructor(public id: number, public handler: TypeHandler<any>) {
            // empty
        }
        write(value: any, writer: Writer): void {
            writer.writeVarInt(this.id);
            this.handler.write(value, writer);
        }
    }

    class NullTypeHandler implements TypeHandler<any> {
        read(reader: Reader): any {
            return null;
        }
        write(value: any, writer: Writer): void {
            // empty
        }
    }
    var NULL_DESC = new TypeDesc(0, new NullTypeHandler);

    class ListTypeHandler implements TypeHandler<any[]> {
        read(reader: Reader, id2typeHandler: TypeHandler<any>[]): any[] {
            var list: any[] = [];
            for (var size = reader.readVarInt(); size > 0; size--) {
                list.push(read(reader, id2typeHandler));
            }
            return list;
        }
        write(value: any[], writer: Writer): void {
            writer.writeVarInt(value.length);
            value.forEach(element => write(element, writer));
        }
    }
    export var LIST_DESC = new TypeDesc(2, new ListTypeHandler);

    class BooleanTypeHandler implements TypeHandler<boolean> {
        read(reader: Reader): boolean {
            return reader.readByte() !== 0;
        }
        write(value: boolean, writer: Writer): void {
            writer.writeByte(value ? 1 : 0);
        }
    }
    export var BOOLEAN_DESC = new TypeDesc(3, new BooleanTypeHandler);

    class NumberTypeHandler implements TypeHandler<number> {
        read(reader: Reader): number {
            return new DataView(reader.array.buffer).getFloat64(reader.needed(8));
        }
        write(value: number, writer: Writer): void {
            var position = writer.needed(8);
            new DataView(writer.array.buffer).setFloat64(position, value);
        }
    }
    export var NUMBER_DESC = new TypeDesc(4, new NumberTypeHandler);

    class StringTypeHandler implements TypeHandler<string> {
        read(reader: Reader): string {
            return reader.readUtf8(reader.readVarInt());
        }
        write(value: string, writer: Writer): void {
            writer.writeVarInt(Writer.calcUtf8bytes(value));
            writer.writeUtf8(value);
        }
    }
    export var STRING_DESC = new TypeDesc(5, new StringTypeHandler);

    class BytesTypeHandler implements TypeHandler<Uint8Array> {
        read(reader: Reader): Uint8Array {
            var length = reader.readVarInt();
            return new Uint8Array(reader.array.buffer, reader.needed(length), length);
        }
        write(value: Uint8Array, writer: Writer): void {
            writer.writeVarInt(value.length);
            var position = writer.needed(value.length);
            writer.array.set(value, position);
        }
    }
    export var BYTES_DESC = new TypeDesc(6, new BytesTypeHandler);

    export var FIRST_ID = 7;

    class EnumTypeHandler implements TypeHandler<Enum> {
        constructor(private values: Enum[]) {
            // empty
        }
        read(reader: Reader): Enum {
            return this.values[reader.readVarInt()];
        }
        write(value: Enum, writer: Writer): void {
            writer.writeVarInt(value.number);
        }
    }

    function read(reader: Reader, id2typeHandler: TypeHandler<any>[]): any {
        return id2typeHandler[reader.readVarInt()].read(reader, id2typeHandler);
    }

    function write(value: any, writer: Writer): void {
        if ((value === null) || (value === undefined)) {
            NULL_DESC.write(null, writer);
        } else if (typeof(value) === "number") {
            NUMBER_DESC.write(value, writer);
        } else if (typeof(value) === "string") {
            STRING_DESC.write(value, writer);
        } else if (typeof(value) === "boolean") {
            BOOLEAN_DESC.write(value, writer);
        } else if (Array.isArray(value)) {
            LIST_DESC.write(value, writer);
        } else if (value instanceof Type) {
            value.constructor.TYPE_DESC.write(value, writer);
        } else if (value instanceof Uint8Array) {
            BYTES_DESC.write(value, writer);
        } else {
            throw new Error("unexpected value '" + value + "'");
        }
    }

    class FieldHandler {
        constructor(private field: string, private typeHandler: TypeHandler<any>) {
            // empty
        }
        read(object: any, reader: Reader, id2typeHandler: TypeHandler<any>[]): any {
            object[this.field] = this.typeHandler ? this.typeHandler.read(reader, id2typeHandler) : read(reader, id2typeHandler);
        }
        write(id: number, object: any, writer: Writer) {
            var value = object[this.field];
            if (value) {
                writer.writeVarInt(id);
                if (this.typeHandler) {
                    this.typeHandler.write(value, writer);
                } else {
                    write(value, writer);
                }
            }
        }
    }

    class ClassTypeHandler implements TypeHandler<any> {
        private fieldId2handler: FieldHandler[] = [];
        constructor(private Constructor: any) {
            // empty
        }
        addField(id: number, handler: FieldHandler): void {
            this.fieldId2handler[id] = handler;
        }
        read(reader: Reader, id2typeHandler: TypeHandler<any>[]): any {
            var object = new this.Constructor;
            while (true) {
                var id = reader.readVarInt();
                if (id === 0) {
                    return object;
                }
                this.fieldId2handler[id].read(object, reader, id2typeHandler);
            }
        }
        write(value: any, writer: Writer): void {
            this.fieldId2handler.forEach((handler, id) => handler.write(id, value, writer));
            writer.writeVarInt(0);
        }
    }

    export interface Serializer {
        read(reader: Reader): any;
        write(value: any, writer: Writer): void;
    }

    export class JsFastSerializer implements Serializer {
        private id2typeHandler: TypeHandler<any>[] = [];
        constructor(...Types: any[]) {
            var add = (typeDesc: TypeDesc) => {
                if (this.id2typeHandler[typeDesc.id]) {
                    throw new Error("TypeDesc with id " + typeDesc.id + " already added");
                }
                this.id2typeHandler[typeDesc.id] = typeDesc.handler;
            };
            [NULL_DESC, LIST_DESC, BOOLEAN_DESC, NUMBER_DESC, STRING_DESC, BYTES_DESC].forEach(add);
            Types.forEach(Type => add(Type.TYPE_DESC));
        }
        read(reader: Reader): any {
            return read(reader, this.id2typeHandler);
        }
        write(value: any, writer: Writer): void {
            write(value, writer);
        }
    }

    export class FieldDesc {
        constructor(public id: number, public name: string, public typeDesc: TypeDesc) {
            // empty
        }
    }

    export function classDesc(id: number, Type: any, ...fieldDescs: FieldDesc[]): TypeDesc {
        var handler = new ClassTypeHandler(Type);
        fieldDescs.forEach(fieldDesc => {
            var typeDesc = fieldDesc.typeDesc;
            handler.addField(fieldDesc.id, new FieldHandler(fieldDesc.name, typeDesc && typeDesc.handler));
        });
        return new TypeDesc(id, handler);
    }

    export function enumDesc(id: number, Type: any): TypeDesc {
        var values: Enum[] = [];
        Object.keys(Type).forEach(property => {
            var value = Type[property];
            if (value instanceof Enum) {
                values[value.number] = value;
            }
        });
        return new TypeDesc(id, new EnumTypeHandler(values));
    }

    export interface Invocation {
        (): any;
    }

    /**
     * An invocation has a Promise if and only if it is a client side rpc style invocation.
     * If an invocation has a Promise, the interceptor will be called twice (first with PromiseEntry and then with PromiseExit).
     * If an invocation doesn't have a Promise, the interceptor will be called once (with NoPromise).
     */
    export enum InvokeStyle { NoPromise, PromiseEntry, PromiseExit }

    export interface Interceptor {
        /**
         * @return invocation()
         */
        (style: InvokeStyle, method: string, parameters: any[], invocation: Invocation): any;
    }

    export var DIRECT: Interceptor = (style, method, parameters, invocation) => invocation();

    export function composite(...interceptors: Interceptor[]): Interceptor {
        function composite2(interceptor1: Interceptor, interceptor2: Interceptor): Interceptor {
            return (style, method, parameters, invocation) => interceptor1(
                style, method, parameters, () => interceptor2(style, method, parameters, invocation)
            );
        }
        var i1 = DIRECT;
        for (var i = 0; i < interceptors.length; i++) {
            var i2 = interceptors[i];
            i1 = (i1 === DIRECT) ? i2 : ((i2 === DIRECT) ? i1 : composite2(i1, i2));
        }
        return i1;
    }

    export interface Message {
        // empty
    }

    export class Request implements Message {
        constructor(public serviceId: number, public methodId: number, public parameters: any[]) {
            // empty
        }
    }

    export interface Reply extends Message {
        process(): any
    }

    export class ValueReply implements Reply {
        constructor(public value: any) {
            // empty
        }
        process(): any {
            return this.value;
        }
    }

    export class ExceptionReply implements Reply {
        constructor(public exception: any) {
            // empty
        }
        process(): any {
            throw this.exception;
        }
    }

    export class MessageSerializer implements Serializer {
        private static REQUEST = 0;
        private static VALUE_REPLY = 1;
        private static EXCEPTION_REPLY = 2;
        constructor(private contractSerializer: Serializer) {
            // empty
        }
        read(reader: Reader): Message {
            var type = reader.readByte();
            if (type === MessageSerializer.REQUEST) {
                return new Request(
                    reader.readZigZagInt(),
                    reader.readZigZagInt(),
                    this.contractSerializer.read(reader)
                );
            } else if (type === MessageSerializer.VALUE_REPLY) {
                return new ValueReply(
                    this.contractSerializer.read(reader)
                );
            } else {
                return new ExceptionReply(
                    this.contractSerializer.read(reader)
                );
            }
        }
        write(message: Message, writer: Writer): void {
            if (message instanceof Request) {
                writer.writeByte(MessageSerializer.REQUEST);
                writer.writeZigZagInt((<Request>message).serviceId);
                writer.writeZigZagInt((<Request>message).methodId);
                this.contractSerializer.write((<Request>message).parameters, writer);
            } else if (message instanceof ValueReply) {
                writer.writeByte(MessageSerializer.VALUE_REPLY);
                this.contractSerializer.write((<ValueReply>message).value, writer);
            } else {
                writer.writeByte(MessageSerializer.EXCEPTION_REPLY);
                this.contractSerializer.write((<ExceptionReply>message).exception, writer);
            }
        }
    }

    export class MethodMapping {
        constructor(public method: string, public id: number, public oneWay: boolean) {
            // empty
        }
    }

    export class MethodMapper<C> {
        private id2mapping: MethodMapping[] = [];
        private name2Mapping: {[methodName: string]: MethodMapping} = {};
        constructor(...mappings: MethodMapping[]) {
            mappings.forEach(mapping => {
                this.id2mapping[mapping.id] = mapping;
                this.name2Mapping[mapping.method] = mapping;
            });
        }
        mapId(id: number): MethodMapping {
            return this.id2mapping[id];
        }
        mapMethod(method: string): MethodMapping {
            return this.name2Mapping[method];
        }
        proxy(interceptor: (method: string, parameters: any[]) => any): C {
            var stub: any = {};
            Object.keys(this.name2Mapping).forEach(method => stub[method] = (...parameters: any[]) => interceptor(method, parameters));
            return stub;
        }
    }

    export class ContractId<C, PC> {
        constructor(public id: number, public methodMapper: MethodMapper<C>) {
            // empty
        }
    }

    export class Service<C> {
        interceptor: Interceptor;
        constructor(public contractId: ContractId<C, any>, public implementation: C, ...interceptors: Interceptor[]) {
            this.interceptor = composite.apply(null, interceptors);
        }
    }

    export class ServerInvoker {
        methodMapper: MethodMapper<any>;
        private interceptor: Interceptor;
        private implementation: any;
        constructor(service: Service<any>) {
            this.methodMapper = service.contractId.methodMapper;
            this.interceptor = service.interceptor;
            this.implementation = service.implementation;
        }
        invoke(interceptor: Interceptor, method: string, parameters: any[]): Reply {
            try {
                return new ValueReply(composite(interceptor, this.interceptor)(InvokeStyle.NoPromise, method, parameters, () => {
                    var result = this.implementation[method].apply(this.implementation, parameters);
                    return result ? result : null;
                }));
            } catch (exception) {
                return new ExceptionReply(exception);
            }
        }
    }

    export class ServerInvocation {
        oneWay: boolean;
        private method: string;
        constructor(private serverInvoker: ServerInvoker, private request: Request) {
            var methodMapping = serverInvoker.methodMapper.mapId(request.methodId);
            this.oneWay = methodMapping.oneWay;
            this.method = methodMapping.method;
        }
        invoke(interceptor: Interceptor): Reply {
            return this.serverInvoker.invoke(interceptor, this.method, this.request.parameters);
        }
    }

    export interface Server {
        (request: Request): ServerInvocation;
    }

    export function server(...services: Service<any>[]): Server {
        var serviceId2invoker: ServerInvoker[] = [];
        services.forEach(service => {
            var id = service.contractId.id;
            if (serviceId2invoker[id]) {
                throw new Error("serviceId " + id + " already added");
            }
            serviceId2invoker[id] = new ServerInvoker(service);
        });
        return request => {
            var invoker = serviceId2invoker[request.serviceId];
            if (!invoker) {
                throw new Error("no serviceId " + request.serviceId + " found (methodId " + request.methodId + ")");
            }
            return new ServerInvocation(invoker, request);
        };
    }

    export interface ProxyFactory {
        proxy<PC>(contractId: ContractId<any, PC>, ...interceptors: Interceptor[]): PC;
    }

    export class Rpc {
        promise: Promise<any>;
        settle: (reply: Reply) => void;
        constructor(interceptor: Interceptor, method: string, parameters: any[]) {
            this.promise = new Promise<any>((resolve, reject) => {
                this.settle = reply => {
                    try {
                        interceptor(InvokeStyle.PromiseExit, method, parameters, () => reply.process());
                    } catch (ignore) {
                        // empty
                    }
                    try {
                        resolve(reply.process());
                    } catch (exception) {
                        reject(exception);
                    }
                };
            });
        }
    }

    export interface Tunnel {
        (request: Request, rpc: Rpc): void;
    }

    export class ClientInvocation {
        constructor(private interceptor: Interceptor, private serviceId: number, private methodMapping: MethodMapping, private parameters: any[]) {
            // empty
        }
        invoke(interceptor: Interceptor, tunnel: Tunnel): Promise<any> {
            var compositeInterceptor = composite(interceptor, this.interceptor);
            var rpc: Rpc = this.methodMapping.oneWay ? null : new Rpc(compositeInterceptor, this.methodMapping.method, this.parameters);
            compositeInterceptor(
                this.methodMapping.oneWay ? InvokeStyle.NoPromise : InvokeStyle.PromiseEntry,
                this.methodMapping.method,
                this.parameters,
                (): any => {
                    tunnel(new Request(this.serviceId, this.methodMapping.id, this.parameters), rpc);
                    return null;
                }
            );
            return rpc ? rpc.promise : null;
        }
    }

    export class Client implements ProxyFactory {
        constructor(private clientInvoker: (invocation: ClientInvocation) => Promise<any>) {
            // empty
        }
        proxy<PC>(contractId: ContractId<any, PC>, ...interceptors: Interceptor[]): PC {
            var interceptor = composite.apply(null, interceptors);
            return <any>contractId.methodMapper.proxy((method, parameters) => this.clientInvoker(
                new ClientInvocation(interceptor, contractId.id, contractId.methodMapper.mapMethod(method), parameters)
            ));
        }
    }

    export class Packet {
        static END_REQUESTNUMBER = 0;
        static END = new Packet(Packet.END_REQUESTNUMBER, null);
        constructor(public requestNumber: number, public message: Message) {
            // empty
        }
        isEnd(): boolean {
            return this.requestNumber === Packet.END_REQUESTNUMBER;
        }
    }

    export class PacketSerializer implements Serializer {
        constructor(private messageSerializer: Serializer) {
            // empty
        }
        read(reader: Reader): Packet {
            var requestNumber = reader.readInt();
            return (requestNumber === Packet.END_REQUESTNUMBER) ? Packet.END : new Packet(requestNumber, this.messageSerializer.read(reader));
        }
        write(packet: Packet, writer: Writer): void {
            if (packet.isEnd()) {
                writer.writeInt(Packet.END_REQUESTNUMBER);
            } else {
                writer.writeInt(packet.requestNumber);
                this.messageSerializer.write(packet.message, writer);
            }
        }
    }

    export interface Session {
        opened(): void;
        /**
         * @param exception null if regular close else reason for close
         */
        closed(exception: any): void;
    }

    export interface SessionFactory {
        (sessionClient: SessionClient): Session;
    }

    export interface Connection {
        write(packet: Packet): void;
        closed(): void;
    }

    /**
     * The session of the active invocation or null if no active invocation.
     */
    export var SESSION: Session = null;
    function sessionInterceptor(session: Session): Interceptor {
        return (style, method, parameters, invocation) => {
            var oldSession = SESSION;
            SESSION = session;
            try {
                return invocation();
            } finally {
                SESSION = oldSession;
            }
        };
    }

    export class SessionClient extends Client {
        private closed = false;
        private requestNumber = Packet.END_REQUESTNUMBER;
        private requestNumber2rpc: Rpc[] = [];
        private session: Session;
        private interceptor: Interceptor;
        constructor(private server: Server, sessionFactory: SessionFactory, private connection: Connection) {
            super(function (invocation: ClientInvocation) {
                return invocation.invoke(this.interceptor, (request, rpc) => {
                    if (this.requestNumber === 2147483647) {
                        this.requestNumber = Packet.END_REQUESTNUMBER;
                    }
                    this.requestNumber++;
                    if (rpc) {
                        if (this.requestNumber2rpc[this.requestNumber]) {
                            throw new Error("already waiting for requestNumber " + this.requestNumber);
                        }
                        this.requestNumber2rpc[this.requestNumber] = rpc;
                    }
                    this.write(this.requestNumber, request);
                });
            });
            this.session = sessionFactory(this);
            this.interceptor = sessionInterceptor(this.session);
            this.session.opened();
        }
        doClose(exception: any): void {
            this.doCloseSend(false, exception);
        }
        private doCloseSend(sendEnd: boolean, exception: any): void {
            if (this.closed) {
                return;
            }
            this.closed = true;
            try {
                this.session.closed(exception);
                if (sendEnd) {
                    this.connection.write(Packet.END);
                }
            } finally {
                this.connection.closed();
            }
        }
        private write(requestNumber: number, message: Message): void {
            if (this.closed) {
                throw new Error("session is already closed");
            }
            try {
                this.connection.write(new Packet(requestNumber, message));
            } catch (exception) {
                this.doClose(exception);
            }
        }
        close(): void {
            this.doCloseSend(true, null);
        }
        received(packet: Packet): void {
            try {
                if (packet.isEnd()) {
                    this.doClose(null);
                    return;
                }
                if (packet.message instanceof Request) {
                    var invocation = this.server(<Request>packet.message);
                    var reply = invocation.invoke(this.interceptor);
                    if (!invocation.oneWay) {
                        this.write(packet.requestNumber, reply);
                    }
                } else { // Reply
                    var rpc = this.requestNumber2rpc[packet.requestNumber];
                    delete this.requestNumber2rpc[packet.requestNumber];
                    rpc.settle(<Reply>packet.message);
                }
            } catch (exception) {
                this.doClose(exception);
            }
        }
    }

    export function writeTo(serializer: Serializer, value: any): Uint8Array {
        var writer = new Writer(128);
        serializer.write(value, writer);
        return writer.getArray();
    }

    export function readFrom(serializer: Serializer, arrayBuffer: ArrayBuffer): any {
        var reader = new Reader(arrayBuffer);
        var value = serializer.read(reader);
        if (!reader.isEmpty()) {
            throw new Error("reader is not empty");
        }
        return value;
    }

    export function connect(url: string, serializer: Serializer, server: Server, sessionFactory: SessionFactory, connectFailed = () => {}): void {
        serializer = new PacketSerializer(new MessageSerializer(serializer));
        var connectFailedCalled = false;
        function callConnectFailed(): void {
            if (!connectFailedCalled) {
                connectFailedCalled = true;
                connectFailed();
            }
        }
        var ws = new WebSocket(url);
        ws.binaryType = "arraybuffer";
        ws.onerror = callConnectFailed;
        ws.onclose = callConnectFailed;
        ws.onopen = () => {
            var sessionClient = new SessionClient(server, sessionFactory, {
                write: packet => ws.send(writeTo(serializer, packet)),
                closed: () => ws.close()
            });
            ws.onmessage = event => {
                try {
                    sessionClient.received(readFrom(serializer, event.data));
                } catch (exception) {
                    sessionClient.doClose(exception);
                }
            };
            ws.onerror = () => sessionClient.doClose(new Error("WebSocket.onerror"));
            ws.onclose = () => sessionClient.doClose(new Error("WebSocket.onclose"));
        };
    }

    class XhrClient extends Client {
        constructor(url: string, serializer: Serializer) {
            super(function (invocation: ClientInvocation) {
                return invocation.invoke(DIRECT, (request, rpc) => {
                    if (!rpc) {
                        throw new Error("xhr not allowed for oneway method (serviceId " + request.serviceId + ", methodId " + request.methodId + ")");
                    }
                    var xhr = new XMLHttpRequest();
                    xhr.open("POST", url);
                    xhr.responseType = "arraybuffer";
                    xhr.onerror = () => rpc.settle(new ExceptionReply(new Error("XMLHttpRequest.onerror")));
                    xhr.onload = () => {
                        try {
                            rpc.settle(readFrom(serializer, xhr.response));
                        } catch (exception) {
                            rpc.settle(new ExceptionReply(exception));
                        }
                    };
                    xhr.send(writeTo(serializer, request));
                });
            });
            serializer = new MessageSerializer(serializer);
        }
    }

    export function xhr(url: string, serializer: Serializer): ProxyFactory {
        return new XhrClient(url, serializer);
    }

}
