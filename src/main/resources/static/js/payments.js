(function () {
    let currentStatusFilter = 'ALL';

    function setStatusFilter(status, button) {
        currentStatusFilter = status;
        document.querySelectorAll('.payment-filter').forEach(item => item.classList.remove('is-active'));
        if (button) button.classList.add('is-active');
        filterPagos();
    }

    function filterPagos() {
        const input = document.getElementById('searchInput');
        const searchValue = input ? input.value.toLowerCase().trim() : '';
        const methodValue = document.getElementById('methodFilter')?.value || '';
        const conceptValue = document.getElementById('conceptFilter')?.value || '';
        const issuedFrom = document.getElementById('issuedFromFilter')?.value || '';
        const issuedTo = document.getElementById('issuedToFilter')?.value || '';
        let visibleRows = 0;

        document.querySelectorAll('.payment-row').forEach(row => {
            const status = row.getAttribute('data-status') || '';
            const method = row.getAttribute('data-method') || '';
            const concept = row.getAttribute('data-concept') || '';
            const issuedDate = row.getAttribute('data-issued') || '';
            const matchesStatus = currentStatusFilter === 'ALL' || status === currentStatusFilter;
            const matchesMethod = methodValue === '' || method === methodValue;
            const matchesConcept = conceptValue === '' || concept === conceptValue;
            const matchesFrom = issuedFrom === '' || (issuedDate !== '' && issuedDate >= issuedFrom);
            const matchesTo = issuedTo === '' || (issuedDate !== '' && issuedDate <= issuedTo);
            const matchesSearch = searchValue === '' || row.textContent.toLowerCase().includes(searchValue);
            const visible = matchesStatus && matchesMethod && matchesConcept && matchesFrom && matchesTo && matchesSearch;
            row.hidden = !visible;
            if (visible) visibleRows++;
        });

        const emptyFilteredRow = document.getElementById('paymentNoResults');
        if (emptyFilteredRow) emptyFilteredRow.hidden = visibleRows !== 0;
        const resultCount = document.getElementById('filterResultCount');
        if (resultCount) resultCount.textContent = `Mostrando ${visibleRows} ${visibleRows === 1 ? 'registro' : 'registros'}`;
    }

    function clearFilters() {
        const searchInput = document.getElementById('searchInput');
        if (searchInput) searchInput.value = '';
        ['methodFilter', 'conceptFilter', 'issuedFromFilter', 'issuedToFilter'].forEach(id => {
            const control = document.getElementById(id);
            if (control) control.value = '';
        });
        const allButton = document.querySelector('.payment-filter[data-filter="ALL"]');
        setStatusFilter('ALL', allButton);
    }

    window.setStatusFilter = setStatusFilter;
    window.filterPagos = filterPagos;
    window.clearFilters = clearFilters;
    document.addEventListener('DOMContentLoaded', filterPagos);
})();
