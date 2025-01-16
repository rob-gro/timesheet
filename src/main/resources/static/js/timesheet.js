document.addEventListener('DOMContentLoaded', function() {
    const form = document.getElementById('timesheet'); // zmieniona nazwa id
    const successAlert = document.getElementById('success');

    form?.addEventListener('submit', function(e) {
        e.preventDefault(); // zatrzymaj domyślną akcję formularza

        const submitButton = form.querySelector('button[type="submit"]');
        submitButton.disabled = true; // Wyłącz przycisk, aby zapobiec wielokrotnemu kliknięciu

        const formData = {
            clientId: document.querySelector('select[name="clientId"]').value,
            serviceDate: document.querySelector('input[name="serviceDate"]').value,
            duration: parseFloat(document.querySelector('input[name="duration"]').value)
        };

        fetch('/api/v1/timesheets', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(formData)
        })
            .then(response => {
                if (response.ok) {
                    successAlert.style.display = 'block';
                    setTimeout(() => {
                        successAlert.style.display = 'none';
                        form.reset();
                    }, 3000);
                } else {
                    return response.text().then(err => {
                        console.error('Error response:', err);
                    });
                }
            })
            .catch(error => {
                console.error('Fetch error:', error);
            })
            .finally(() => {
                submitButton.disabled = false; // Włącz przycisk ponownie po zakończeniu żądania
            });
    });

    const durationInput = document.querySelector('input[name="duration"]');

    durationInput?.addEventListener('change', function() {
        let value = parseFloat(this.value);
        if (value < 0.5) value = 0.5;
        if (value > 12) value = 12;
        this.value = Math.round(value * 2) / 2;
    });
});

document.addEventListener('DOMContentLoaded', function() {
    const dateInput = document.getElementById('serviceDate');

    // Automatyczne ustawienie domyślnej daty w formacie yyyy-MM-dd
    const today = new Date();
    // const yyyy = today.getFullYear();
    const yyyy = 2023;
    const mm = String(today.getMonth() + 1).padStart(2, '0');
    const dd = String(today.getDate()).padStart(2, '0');
    dateInput.value = `${yyyy}-${mm}-${dd}`; // Ustawienie wartości

    // Walidacja daty przed wysłaniem formularza
    document.getElementById('timesheet').addEventListener('submit', function(e) {
        if (!dateInput.value.match(/^\d{4}-\d{2}-\d{2}$/)) {
            e.preventDefault();
            alert('Proszę wprowadzić datę w formacie yyyy-MM-dd.');
        }
    });
});

function incrementHours() {
    const input = document.querySelector('input[name="duration"]');
    const currentValue = parseFloat(input.value);
    if (currentValue < 12) {
        input.value = (Math.round((currentValue + 0.5) * 2) / 2).toFixed(1);
    }
}

function decrementHours() {
    const input = document.querySelector('input[name="duration"]');
    const currentValue = parseFloat(input.value);
    if (currentValue > 0.5) {
        input.value = (Math.round((currentValue - 0.5) * 2) / 2).toFixed(1);
    }
}
