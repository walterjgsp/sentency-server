package wcode.software.data.database.daos

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import wcode.software.base.BaseDAO
import wcode.software.data.database.schemas.AuthorSchema
import wcode.software.data.database.tables.QuoteDB
import wcode.software.data.database.tables.AuthorDB
import wcode.software.dtos.AuthorDTO
import wcode.software.dtos.QuoteDTO
import java.util.*
import kotlin.collections.ArrayList

class AuthorDAO : BaseDAO<AuthorDTO, AuthorSchema> {

    override fun getAll(): ArrayList<AuthorDTO> {
        val authors = arrayListOf<AuthorDTO>()
        transaction {
            authors.addAll(AuthorSchema.all().map { author ->
                AuthorDTO(author)
            })
        }

        return authors
    }

    override fun getCount(): Int {
        return transaction {
            AuthorSchema.all().count().toInt()
        }
    }

    override fun insert(obj: AuthorDTO) {
        transaction {
            AuthorDB.insert {
                it[name] = obj.name
                it[picUrl] = obj.picUrl
            }
        }
    }

    override fun remove(id: String) {
        transaction {
            val author = AuthorSchema.findById(UUID.fromString(id))
            author?.delete()
        }
    }

    override fun update(obj: AuthorDTO) {
        transaction {
            obj.id?.let {
                val author = AuthorSchema.findById(UUID.fromString(it)) ?: return@transaction
                author.name = obj.name
                author.picUrl = obj.picUrl
            }
        }
    }

    fun getAuthorWithMostQuotes(): AuthorDTO? {
        return transaction {
            val expression = wrapAsExpression<Int>(QuoteDB.slice(QuoteDB.id.count()).select {
                AuthorDB.id eq QuoteDB.author
            })

            val collection = AuthorSchema.all().orderBy(Pair(expression, SortOrder.DESC))
            if (!collection.empty()) {
                val author = AuthorSchema.all().orderBy(Pair(expression, SortOrder.DESC)).first()
                AuthorDTO(author)
            } else {
                null
            }
        }
    }

    fun getQuotesFromAuthor(authorId: String): ArrayList<QuoteDTO> {
        val quotes = arrayListOf<QuoteDTO>()
        transaction {
            val author = AuthorSchema.findById(UUID.fromString(authorId)) ?: return@transaction
            author.quotes.forEach { quote ->
                try {
                    quotes.add(QuoteDTO(quote))
                } catch (e: Exception) {
                    quote.delete()
                }
            }
        }
        return quotes
    }
}
