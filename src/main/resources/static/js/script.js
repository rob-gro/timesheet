document.getElementById('timesheet-form').addEventListener('submit', function (e) {
    e.preventDefault();
    const successAlert = document.getElementById('success');
    successAlert.style.display = 'block';

    setTimeout(() => {
        successAlert.style.display = 'none';
        this.reset();
    }, 3000);
});


function incrementHours() {
    const input = document.querySelector('input[name="hours"]');
    const currentValue = parseFloat(input.value);
    if (currentValue < 12) {
        input.value = (Math.round((currentValue + 0.5) * 2) / 2).toFixed(1);
    }
}

function decrementHours() {
    const input = document.querySelector('input[name="hours"]');
    const currentValue = parseFloat(input.value);
    if (currentValue > 0.5) {
        input.value = (Math.round((currentValue - 0.5) * 2) / 2).toFixed(1);
    }
}

document.querySelector('input[name="hours"]').addEventListener('change', function () {
    let value = parseFloat(this.value);
    if (value < 0.5) value = 0.5;
    if (value > 12) value = 12;
    this.value = Math.round(value * 2) / 2;
});
