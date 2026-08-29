(function () {
    'use strict';

    function ensureCsrfTokens() {
        var tokenMeta = document.querySelector('meta[name="_csrf"]');
        var parameterMeta = document.querySelector('meta[name="_csrf_parameter"]');
        if (!tokenMeta || !parameterMeta || !tokenMeta.content) {
            return;
        }

        document.querySelectorAll('form[method="post"], form[method="POST"], form:not([method])').forEach(function (form) {
            var tokenInput = form.querySelector('input[name="' + parameterMeta.content + '"]');
            if (!tokenInput) {
                tokenInput = document.createElement('input');
                tokenInput.type = 'hidden';
                tokenInput.name = parameterMeta.content;
                tokenInput.value = tokenMeta.content;
                form.appendChild(tokenInput);
            }
        });
    }

    function bindFormValidation() {
        ensureCsrfTokens();
        document.querySelectorAll('form').forEach(function (form) {
            form.addEventListener('submit', function (event) {
                if (!form.checkValidity()) {
                    event.preventDefault();
                    event.stopPropagation();
                }
                form.classList.add('was-validated');
            });
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', bindFormValidation);
    } else {
        bindFormValidation();
    }
}());

function filterRows(group) {
    var search = document.querySelector('[data-filter-search="' + group + '"]');
    var statusControl = document.querySelector('[data-filter-status="' + group + '"]');
    var query = search ? search.value.toLowerCase().trim() : '';
    var status = statusControl ? statusControl.value : 'ALL';
    document.querySelectorAll('[data-filter-row="' + group + '"]').forEach(function (row) {
        var matchesText = !query || row.textContent.toLowerCase().includes(query);
        var matchesStatus = status === 'ALL' || row.dataset.status === status;
        row.hidden = !(matchesText && matchesStatus);
    });
}

var currentStatusFilter = 'ALL';

function setStatusFilter(status) {
    currentStatusFilter = status;
    filterPagos();
}

function filterPagos() {
    var searchInput = document.getElementById('searchInput');
    var searchValue = searchInput ? searchInput.value.toLowerCase().trim() : '';
    document.querySelectorAll('.pago-row').forEach(function (row) {
        var status = row.getAttribute('data-status') || '';
        var text = (row.getAttribute('data-residente') || '') + ' '
            + (row.getAttribute('data-apartamento') || '') + ' ' + status;
        var statusMatch = currentStatusFilter === 'ALL' || status === currentStatusFilter;
        row.hidden = !(statusMatch && (!searchValue || text.toLowerCase().includes(searchValue)));
    });
}
