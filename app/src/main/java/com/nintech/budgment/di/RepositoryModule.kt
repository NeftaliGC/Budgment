package com.nintech.budgment.di

import com.nintech.budgment.data.repository.AccountRepository
import com.nintech.budgment.data.repository.AccountRepositoryImpl
import com.nintech.budgment.data.repository.BudgetRepository
import com.nintech.budgment.data.repository.BudgetRepositoryImpl
import com.nintech.budgment.data.repository.AuthRepository
import com.nintech.budgment.data.repository.AuthRepositoryImpl
import com.nintech.budgment.data.repository.CategoryRepository
import com.nintech.budgment.data.repository.CategoryRepositoryImpl
import com.nintech.budgment.data.repository.ExchangeRateRepository
import com.nintech.budgment.data.repository.ExchangeRateRepositoryImpl
import com.nintech.budgment.data.repository.TransactionRepository
import com.nintech.budgment.data.repository.TransactionRepositoryImpl
import com.nintech.budgment.data.repository.TransferRepository
import com.nintech.budgment.data.repository.RecurringRepository
import com.nintech.budgment.data.repository.RecurringRepositoryImpl
import com.nintech.budgment.data.repository.TransferRepositoryImpl
import com.nintech.budgment.data.repository.UserRepository
import com.nintech.budgment.data.repository.UserRepositoryImpl
import com.nintech.budgment.data.session.SessionManager
import com.nintech.budgment.data.session.SessionManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindAccountRepository(impl: AccountRepositoryImpl): AccountRepository

    @Binds
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindSessionManager(impl: SessionManagerImpl): SessionManager

    @Binds
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds
    abstract fun bindTransactionRepository(impl: TransactionRepositoryImpl): TransactionRepository

    @Binds
    abstract fun bindTransferRepository(impl: TransferRepositoryImpl): TransferRepository

    @Binds
    abstract fun bindRecurringRepository(impl: RecurringRepositoryImpl): RecurringRepository

    @Binds
    abstract fun bindBudgetRepository(impl: BudgetRepositoryImpl): BudgetRepository

    @Binds
    @Singleton
    abstract fun bindExchangeRateRepository(impl: ExchangeRateRepositoryImpl): ExchangeRateRepository
}