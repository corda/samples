package com.gitcoins.utilities

import co.paralleluniverse.fibers.Suspendable
import com.gitcoins.schema.GitUserMappingSchemaV1
import net.corda.core.node.ServiceHub

class QueryGitUserDatabase {

    @Suspendable
    fun listEntriesForGitUserName(gitUserName: String, serviceHub: ServiceHub): MutableList<GitUserMappingSchemaV1.GitUserKeys> {
        return serviceHub.withEntityManager {
            val query = criteriaBuilder.createQuery(GitUserMappingSchemaV1.GitUserKeys::class.java)
            val gitUserMapping = query.from(GitUserMappingSchemaV1.GitUserKeys::class.java)
            query.where(criteriaBuilder.equal(gitUserMapping.get<String>("gitUserName"), gitUserName))
            createQuery(query).resultList
        }
    }
}