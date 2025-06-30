import {Component} from '@angular/core';
import {HttpClient} from '@angular/common/http';

interface TransactionChain {
    firstId: string;
    transactions: Transaction[];
}

interface Transaction {
    primaryId: string;
    secondaryId: string;
    eventType: string;
    date: string;
}

@Component({
    selector: 'app-transaction-chain',
    templateUrl: './transaction-chain.component.html',
    styleUrls: ['./transaction-chain.component.css']
})
export class TransactionChainComponent {
    searchId: string = '';
    transactions: Transaction[] = [];
    errorTransactions: Transaction[] = [];

    constructor(private http: HttpClient) {
    }

    searchChain() {
        if (!this.searchId.trim()) {
            return alert('Veuillez entrer un ID à rechercher.');
        }
        this.http.get<TransactionChain>(`/api/transactions/reconciled/${this.searchId}`).subscribe(transactionChain => {
            this.transactions = this.buildChain(this.searchId, transactionChain.transactions);
            this.errorTransactions = transactionChain.transactions.filter(t => t.date.includes('33:')); // Mauvais timestamp simulé
        });
    }

    buildChain(startId: string, data: Transaction[]): Transaction[] {
        const map = new Map(data.map(t => [t.primaryId, t]));
        const chain: Transaction[] = [];
        let current = map.get(startId);
        while (current) {
            chain.push(current);
            current = data.find(t => t.secondaryId === current.primaryId);
        }
        return chain;
    }

    isDateBefore(current: string, previous: string): boolean {
        return new Date(current).getTime() < new Date(previous).getTime();
    }

    formatDate(date: string): Date {
        return new Date(date);
    }
}