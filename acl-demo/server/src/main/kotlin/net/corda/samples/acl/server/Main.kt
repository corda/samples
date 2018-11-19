package net.corda.samples.acl.server

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import net.corda.core.identity.CordaX500Name
import java.io.File
import java.net.InetSocketAddress

const val DEFAULT_ACL_FILE_NAME = "acl.txt"

class AccessControlList(private val file: File) {
    val list: Set<CordaX500Name> get() = readListFromFile()
    private fun readListFromFile() = file.readLines().map { CordaX500Name.parse(it) }.toSet()
}

fun parseArgs(args: Array<String>) = when (args.size) {
    0 -> DEFAULT_ACL_FILE_NAME
    1 -> args[1]
    else -> throw IllegalStateException("Only one optional argument is required.")
}

fun main(args: Array<String>) {
    val file = File(parseArgs(args))
    println("This program serves an access control list of CordaX500Names on localhost:8000.")
    println("WARNING: \"$file\" must contain correctly formatted CordaX500Names.")
    val acl = AccessControlList(file)
    val handler = AclHandler(acl)
    createHttpServer(handler)
    println("Press Ctrl+C to quit.")
}

fun createHttpServer(handler: HttpHandler) {
    val server = HttpServer.create(InetSocketAddress(8000), 0)
    server.createContext("/acl", handler)
    server.executor = null
    server.start()
}

class AclHandler(val acl: AccessControlList) : HttpHandler {
    override fun handle(request: HttpExchange) {
        val (list, error) = try {
            Pair(acl.list.joinToString("\n"), false)
        } catch (e: Throwable) {
            Pair("The access control list contains malformed CordaX500Names", true)
        }
        if (error) {
            request.sendResponseHeaders(500, 0)
            val response = request.responseBody
            response.close()
        } else {
            request.sendResponseHeaders(200, list.length.toLong())
            val response = request.responseBody
            response.write(list.toByteArray())
            response.close()
        }
    }
}


