// Smooth scrolling for anchor links
document.querySelectorAll('a[href^="#"]').forEach(anchor => {
    anchor.addEventListener('click', function (e) {
        e.preventDefault();
        const target = document.querySelector(this.getAttribute('href'));
        if (target) {
            target.scrollIntoView({
                behavior: 'smooth',
                block: 'start'
            });
        }
    });
});

// Navbar scroll effect
let lastScroll = 0;
const navbar = document.querySelector('.navbar');

window.addEventListener('scroll', () => {
    const currentScroll = window.pageYOffset;
    
    if (currentScroll > 100) {
        navbar.style.boxShadow = '0 4px 6px -1px rgb(0 0 0 / 0.1)';
    } else {
        navbar.style.boxShadow = 'none';
    }
    
    lastScroll = currentScroll;
});

// Animate elements on scroll
const observerOptions = {
    threshold: 0.1,
    rootMargin: '0px 0px -50px 0px'
};

const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
        if (entry.isIntersecting) {
            entry.target.style.opacity = '1';
            entry.target.style.transform = 'translateY(0)';
        }
    });
}, observerOptions);

// Observe all feature cards, pricing cards, etc.
document.querySelectorAll('.feature-card, .pricing-card, .role-card, .faq-item').forEach(el => {
    el.style.opacity = '0';
    el.style.transform = 'translateY(20px)';
    el.style.transition = 'opacity 0.6s ease, transform 0.6s ease';
    observer.observe(el);
});

// Track CTA button clicks
document.querySelectorAll('.btn-primary, .btn-secondary').forEach(button => {
    button.addEventListener('click', (e) => {
        // Analytics tracking would go here
        console.log('Button clicked:', button.textContent);
    });
});

// FAQ accordion (if needed in future)
document.querySelectorAll('.faq-item').forEach(item => {
    item.addEventListener('click', () => {
        item.classList.toggle('active');
    });
});

// Mobile menu toggle (for future implementation)
const createMobileMenu = () => {
    const navLinks = document.querySelector('.nav-links');
    const menuButton = document.createElement('button');
    menuButton.className = 'mobile-menu-button';
    menuButton.innerHTML = '☰';
    menuButton.style.display = 'none';
    
    if (window.innerWidth <= 768) {
        menuButton.style.display = 'block';
        document.querySelector('.nav-content').insertBefore(
            menuButton,
            document.querySelector('.nav-actions')
        );
    }
    
    menuButton.addEventListener('click', () => {
        navLinks.classList.toggle('mobile-open');
    });
};

// Initialize on load
window.addEventListener('load', () => {
    createMobileMenu();
});

// Pricing plan selection tracking
document.querySelectorAll('.pricing-card a').forEach(link => {
    link.addEventListener('click', (e) => {
        const plan = link.closest('.pricing-card').querySelector('h3').textContent;
        console.log('Selected plan:', plan);
        // Send to analytics
    });
});

// Form validation (if forms are added)
const validateEmail = (email) => {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
};

// Add loading state to buttons
document.querySelectorAll('a[href^="/register"], a[href^="/login"]').forEach(link => {
    link.addEventListener('click', (e) => {
        const button = e.currentTarget;
        button.style.opacity = '0.7';
        button.style.pointerEvents = 'none';
        
        // Reset after navigation
        setTimeout(() => {
            button.style.opacity = '1';
            button.style.pointerEvents = 'auto';
        }, 2000);
    });
});

// Testimonial slider (placeholder for future)
const initTestimonialSlider = () => {
    // Implementation for testimonial carousel
};

// Stats counter animation
const animateStats = () => {
    const stats = document.querySelectorAll('.stat-value');
    
    stats.forEach(stat => {
        const target = parseInt(stat.textContent.replace(/[^0-9]/g, ''));
        const suffix = stat.textContent.replace(/[0-9]/g, '');
        let current = 0;
        const increment = target / 50;
        
        const updateCounter = () => {
            if (current < target) {
                current += increment;
                stat.textContent = Math.ceil(current) + suffix;
                requestAnimationFrame(updateCounter);
            } else {
                stat.textContent = target + suffix;
            }
        };
        
        // Start animation when in viewport
        const statObserver = new IntersectionObserver((entries) => {
            entries.forEach(entry => {
                if (entry.isIntersecting) {
                    updateCounter();
                    statObserver.unobserve(entry.target);
                }
            });
        });
        
        statObserver.observe(stat);
    });
};

// Initialize stats animation
animateStats();

// Console welcome message
console.log('%c🏢 HostelHub', 'font-size: 24px; font-weight: bold; color: #0F172A;');
console.log('%cModern Hostel Management Platform', 'font-size: 14px; color: #64748B;');
console.log('%cInterested in our API? Contact us at api@hostelhub.com', 'font-size: 12px; color: #38BDF8;');
